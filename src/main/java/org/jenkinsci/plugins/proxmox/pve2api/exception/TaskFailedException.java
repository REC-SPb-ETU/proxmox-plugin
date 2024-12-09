package org.jenkinsci.plugins.proxmox.pve2api.exception;

public class TaskFailedException extends PveException {
    
    public TaskFailedException(String message) {
        super(message);
    }

    public TaskFailedException(Throwable cause) {
        super(cause);
    }

    public TaskFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
