package dev.pixeldenseui.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class ModuleConfigTest {
    @Test
    public void safeDefaultsKeepNotificationHooksOff() {
        ModuleConfig config = new ModuleConfig(null);
        assertEquals(ModuleConfig.NOTIFICATION_MODE_OFF, config.notificationMode());
    }

    @Test
    public void cutoutClampIsOptInUntilSystemScopeValidation() {
        ModuleConfig config = new ModuleConfig(null);
        assertFalse(config.clampCutoutSafeInset());
    }
}
