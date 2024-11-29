package org.jenkinsci.plugins.proxmox.pve2api;

import java.io.IOException;

public class QemuErrorException extends IOException {

    public QemuErrorException(String message) {
        super(message);
    }
}
