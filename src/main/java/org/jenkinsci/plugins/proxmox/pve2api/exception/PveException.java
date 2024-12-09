package org.jenkinsci.plugins.proxmox.pve2api.exception;

import java.io.IOException;

public class PveException extends IOException {
    
    public PveException(String message) {
        super(message);
    }

    public PveException(Throwable cause) {
        super(cause);
    }

    public PveException(String message, Throwable cause) {
        super(message, cause);
    }
}
