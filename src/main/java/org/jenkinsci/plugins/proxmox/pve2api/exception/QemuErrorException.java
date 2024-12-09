package org.jenkinsci.plugins.proxmox.pve2api.exception;

public class QemuErrorException extends PveException {

    public QemuErrorException(String message) {
        super(message);
    }

    public QemuErrorException(Throwable cause) {
        super(cause);
    }

    public QemuErrorException(String message, Throwable cause) {
        super(message, cause);
    }
}
