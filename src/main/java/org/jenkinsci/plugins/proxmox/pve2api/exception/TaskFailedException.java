package org.jenkinsci.plugins.proxmox.pve2api.exception;

public class TaskFailedException extends PveException {
    
    public TaskFailedException(String message) {
        super(message);
    }
}
