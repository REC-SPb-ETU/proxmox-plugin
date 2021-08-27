package org.jenkinsci.plugins.proxmox;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.login.LoginException;

import org.jenkinsci.plugins.proxmox.pve2api.Connector;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.slaves.Cloud;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.JNLPLauncher;
import hudson.slaves.SlaveComputer;
import jenkins.model.Jenkins;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

/**
 * Controls launching of Proxmox virtual machines.
 */
public class VirtualMachineLauncher extends ComputerLauncher {

    private static final Logger LOGGER = Logger.getLogger(VirtualMachineLauncher.class.getName());
    private ComputerLauncher delegate;
    private String datacenterDescription;
    private String datacenterNode;
    private Integer virtualMachineId;
    private String snapshotName;
    private Boolean startVM;
    private final int WAIT_TIME_MS;

    public static enum RevertPolicy {

        AFTER_CONNECT("After connect to the virtual machine"),
        BEFORE_JOB("Before every job executing on the virtual machine");

        final private String label;

        private RevertPolicy(String policy) {
            this.label = policy;
        }

        public String getLabel() {
            return label;
        }
    }
    
    private final RevertPolicy revertPolicy;
    
    @DataBoundConstructor
    public VirtualMachineLauncher(ComputerLauncher delegate, String datacenterDescription, String datacenterNode,
                                  Integer virtualMachineId, String snapshotName, Boolean startVM, int waitingTimeSecs,
                                  RevertPolicy revertPolicy) {
        super();
        this.delegate = delegate;
        this.datacenterDescription = datacenterDescription;
        this.datacenterNode = datacenterNode;
        this.virtualMachineId = virtualMachineId;
        this.snapshotName = snapshotName;
        this.startVM = startVM;
        this.WAIT_TIME_MS = waitingTimeSecs*1000;
        this.revertPolicy = revertPolicy;
    }

    public ComputerLauncher getDelegate() {
        return delegate;
    }

    public Datacenter findDatacenterInstance() throws RuntimeException {
        if (datacenterDescription != null && virtualMachineId != null) {
            for (Cloud cloud : Jenkins.get().clouds) {
                if (cloud instanceof Datacenter
                        && ((Datacenter) cloud).getDatacenterDescription().equals(datacenterDescription)) {
                    return (Datacenter) cloud;
                }
            }
        }
        throw new RuntimeException("Could not find the proxmox datacenter instance!");
    }

    @Override
    public boolean isLaunchSupported() {
        //TODO: Add this into the settings for node setup
        boolean overrideLaunchSupported = delegate.isLaunchSupported();
        //Support launching for the JNLPLauncher, so the `launch` function gets called
        //and the VM can be reset to a snapshot.
        if (delegate instanceof JNLPLauncher) {
            overrideLaunchSupported = true;
        }
        return overrideLaunchSupported;
    }

    public void startSlaveIfNeeded(TaskListener taskListener) throws IOException, InterruptedException {
        String taskId = null;
        JSONObject taskStatus = null;
        try {
            Datacenter datacenter = findDatacenterInstance();
            Connector pve = datacenter.proxmoxInstance();
            Boolean isvmIdRunning = pve.isQemuMachineRunning(datacenterNode, virtualMachineId);
            if (!isvmIdRunning) {
                taskListener.getLogger().println("Starting virtual machine...");
                taskId = pve.startQemuMachine(datacenterNode, virtualMachineId);
                taskStatus = pve.waitForTaskToFinish(datacenterNode, taskId);
                taskListener.getLogger().println("Task finished! Status object: " + taskStatus.toString());
            }
        } catch (IOException e) {
            taskListener.getLogger().println("ERROR: IOException: " + e.getMessage());
        } catch (JSONException e) {
            taskListener.getLogger().println("ERROR: Parsing JSON: " + e.getMessage());
        } catch (LoginException e) {
            taskListener.getLogger().println("ERROR: Login failed: " + e.getMessage());
        }
    }

    public void revertSnapshot(SlaveComputer slaveComputer, TaskListener taskListener) throws InterruptedException {
        String taskId = null;
        JSONObject taskStatus = null;

        try {            
            Datacenter datacenter = findDatacenterInstance();
            Connector pve = datacenter.proxmoxInstance();
            
            if (!snapshotName.equals("current")) {
              taskListener.getLogger().println("Virtual machine \"" + virtualMachineId
                  + "\" (Name \"" + slaveComputer.getDisplayName() + "\") is being reverted...");
              //TODO: Check the status of this task (pass/fail) not just that its finished
              taskId = pve.rollbackQemuMachineSnapshot(datacenterNode, virtualMachineId, snapshotName);
              taskListener.getLogger().println("Proxmox returned: " + taskId);
  
              //Wait for the task to finish
              taskStatus = pve.waitForTaskToFinish(datacenterNode, taskId);
              taskListener.getLogger().println("Task finished! Status object: " + taskStatus.toString());
            }
  
            Boolean isvmIdRunning = pve.isQemuMachineRunning(datacenterNode, virtualMachineId);
            if (startVM) {
                startSlaveIfNeeded(taskListener);
            }
  
        } catch (IOException e) {
            taskListener.getLogger().println("ERROR: IOException: " + e.getMessage());
        } catch (JSONException e) {
            taskListener.getLogger().println("ERROR: Parsing JSON: " + e.getMessage());
        } catch (LoginException e) {
            taskListener.getLogger().println("ERROR: Login failed: " + e.getMessage());
        }
  
        //Ignore the wait period for a JNLP agent as it connects back to the Jenkins instance.
        if (!(delegate instanceof JNLPLauncher)) {
            Thread.sleep(WAIT_TIME_MS);
        }
    }
    
    @Override
    public void launch(SlaveComputer slaveComputer, TaskListener taskListener) throws IOException, InterruptedException {
        if (revertPolicy == RevertPolicy.AFTER_CONNECT) {
            revertSnapshot(slaveComputer, taskListener);
        } else {
            if (startVM) {
                startSlaveIfNeeded(taskListener);
            }
        }

        delegate.launch(slaveComputer, taskListener);
    }

    @Override
    public synchronized void afterDisconnect(SlaveComputer slaveComputer, TaskListener taskListener) {
        delegate.afterDisconnect(slaveComputer, taskListener);
    }
    
    public void shutdown(SlaveComputer slaveComputer, TaskListener taskListener) {
        String taskId = null;
        JSONObject taskStatus = null;
        
        //try to gracefully shutdown the virtual machine
        try {
            taskListener.getLogger().println("Virtual machine \"" + virtualMachineId
              + "\" (slave \"" + slaveComputer.getDisplayName() + "\") is being shutdown.");
            Datacenter datacenter = findDatacenterInstance();
            Connector pve = datacenter.proxmoxInstance();
            taskId = pve.shutdownQemuMachine(datacenterNode, virtualMachineId);
            taskStatus = pve.waitForTaskToFinish(datacenterNode, taskId);
            if (!taskStatus.getString("exitstatus").equals("OK")) {
              //Graceful shutdown failed, so doing a stop.
              taskListener.getLogger().println("Virtual machine \"" + virtualMachineId
                  + "\" (slave \"" + slaveComputer.getDisplayName() + "\") was not able to shutdown, doing a stop instead");
              taskId = pve.stopQemuMachine(datacenterNode, virtualMachineId);
              taskStatus = pve.waitForTaskToFinish(datacenterNode, taskId);
            }
            taskListener.getLogger().println("Task finished! Status object: " + taskStatus.toString());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Exception: " + e.getMessage());
        } catch (JSONException e) {
            LOGGER.log(Level.SEVERE, "Parsing JSON: " + e.getMessage());
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Waiting for task completion failed: " + e.getMessage());
        } catch (LoginException e) {
            LOGGER.log(Level.WARNING, "Login failed: " + e.getMessage());
        }
    }

    @Override
    public void beforeDisconnect(SlaveComputer slaveComputer, TaskListener taskListener) {
    	if(revertPolicy == RevertPolicy.AFTER_CONNECT)
    		shutdown(slaveComputer, taskListener);
    	
        delegate.beforeDisconnect(slaveComputer, taskListener);
    }

    @Override
    public Descriptor<ComputerLauncher> getDescriptor() {
        throw new UnsupportedOperationException();
    }
}
