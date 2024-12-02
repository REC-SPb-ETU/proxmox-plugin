package org.jenkinsci.plugins.proxmox.pve2api.exception;

import java.io.IOException;

public class PveException extends IOException {
    
    public PveException(String message) {
        super(message);
    }
}
