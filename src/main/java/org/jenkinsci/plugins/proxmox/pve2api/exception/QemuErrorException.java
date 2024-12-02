package org.jenkinsci.plugins.proxmox.pve2api.exception;

public class QemuErrorException extends PveException {

    public QemuErrorException(String message) {
        super(message);
    }
}
