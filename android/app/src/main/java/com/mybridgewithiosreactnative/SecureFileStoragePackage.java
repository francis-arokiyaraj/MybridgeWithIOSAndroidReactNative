package com.mybridgewithiosreactnative;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.uimanager.ViewManager;
import com.facebook.react.bridge.ReactApplicationContext;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

public class SecureFileStoragePackage implements ReactPackage {
    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
        List<NativeModule> modules = new ArrayList<>();
        modules.add(new SecureFileStorage(reactContext));
        modules.add(new SecureCertificateBridge(reactContext));
        modules.add(new WifiConnector(reactContext));
        modules.add(new SecureHttpSDownloaderModule(reactContext));
        modules.add(new SecureHttpsModule(reactContext));

        return modules;
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
        return Collections.emptyList();
    }
}
