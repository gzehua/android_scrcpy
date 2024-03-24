package com.genymobile.scrcpy.wrappers;

import static android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;

import android.hardware.display.VirtualDisplay;
import android.os.IInterface;
import android.view.Surface;

import com.genymobile.scrcpy.DisplayInfo;
import com.genymobile.scrcpy.Size;

import java.lang.reflect.Method;

public final class DisplayManager {
    private final IInterface manager;
    private Method createVirtualDisplayMethod;

    public DisplayManager(IInterface manager) {
        this.manager = manager;
    }

    public DisplayInfo getDisplayInfo() {
        try {
            Object displayInfo = manager.getClass().getMethod("getDisplayInfo", int.class).invoke(manager, 0);
            Class<?> cls = displayInfo.getClass();
            // width and height already take the rotation into account
            int width = cls.getDeclaredField("logicalWidth").getInt(displayInfo);
            int height = cls.getDeclaredField("logicalHeight").getInt(displayInfo);
            int rotation = cls.getDeclaredField("rotation").getInt(displayInfo);
            return new DisplayInfo(new Size(width, height), rotation);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private Method getCreateVirtualDisplayMethod() throws NoSuchMethodException {
        if (createVirtualDisplayMethod == null) {
            createVirtualDisplayMethod = android.hardware.display.DisplayManager.class.getMethod("createVirtualDisplay", String.class, int.class, int.class, int.class, Surface.class, int.class);
        }
        return createVirtualDisplayMethod;
    }


    public VirtualDisplay createVirtualDisplay(String name, int width, int height, int displayIdToMirror, Surface surface) throws Exception {
        Method method = getCreateVirtualDisplayMethod();
        return (VirtualDisplay) method.invoke(manager, name, width, height, displayIdToMirror, surface, VIRTUAL_DISPLAY_FLAG_PUBLIC);
    }

}
