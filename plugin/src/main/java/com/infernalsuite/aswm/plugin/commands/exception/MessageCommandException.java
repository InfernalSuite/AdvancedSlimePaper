package com.infernalsuite.aswm.plugin.commands.exception;

import net.kyori.adventure.text.Component;

public class MessageCommandException extends RuntimeException {

    private final Component component;

    public MessageCommandException(Component component) {
        this.component = component;
    }

    public Component getComponent() {
        return component;
    }
}
