package com.wechantloup.upnpvideoplayer.browse;

import android.util.Log;

import org.fourthline.cling.android.AndroidUpnpService;
import org.fourthline.cling.binding.xml.DescriptorBindingException;
import org.fourthline.cling.binding.xml.DeviceDescriptorBinder;
import org.fourthline.cling.binding.xml.ServiceDescriptorBinder;
import org.fourthline.cling.model.ValidationError;
import org.fourthline.cling.model.ValidationException;
import org.fourthline.cling.model.message.StreamRequestMessage;
import org.fourthline.cling.model.message.StreamResponseMessage;
import org.fourthline.cling.model.message.UpnpHeaders;
import org.fourthline.cling.model.message.UpnpRequest;
import org.fourthline.cling.model.meta.Icon;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.RemoteDeviceIdentity;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.model.types.ServiceType;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.registry.RegistrationException;
import org.fourthline.cling.transport.RouterException;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RetrieveDeviceThread extends Thread {

    private static final boolean DEBUG = true;
    private static final String TAG = RetrieveDeviceThread.class.getSimpleName();

    private final AndroidUpnpService mUpnpService;
    private final String mUdn;
    private final String mUrl;
    private final int mMaxAge;
    private final RetrieveDeviceThreadListener mListener;
    private RemoteDevice mDevice;

    public RetrieveDeviceThread(AndroidUpnpService upnpService, String udn, String url, int maxAge, RetrieveDeviceThreadListener listener) {
        mUpnpService = upnpService;
        mUdn = udn;
        mUrl = url;
        mMaxAge = maxAge;
        mListener = listener;
    }

    public void run(){
        if (DEBUG)
            Log.i(TAG, "Retrieve remote device");
        try {
            RemoteDeviceIdentity deviceIdentity = new RemoteDeviceIdentity(
                    new UDN(mUdn),
                    mMaxAge,
                    new URL(mUrl),
                    null,
                    InetAddress.getLocalHost()
            );
            mDevice = new RemoteDevice(deviceIdentity);
            if (!describe()) {
                if (DEBUG)
                    Log.i(TAG, "Device not found");
                mListener.onDeviceNotFound();
            }
        } catch (MalformedURLException | UnknownHostException | ValidationException | RouterException e) {
            Log.e(TAG, "Error while retrieving device");
            e.printStackTrace();
            mListener.onDeviceNotFound();
        }
    }

    private boolean describe() throws RouterException {

        // All of the following is a very expensive and time consuming procedure, thanks to the
        // braindead design of UPnP. Several GET requests, several descriptors, several XML parsing
        // steps - all of this could be done with one and it wouldn't make a difference. So every
        // call of this method has to be really necessary and rare.

        if(mUpnpService.get().getRouter() == null) {
            Log.w(TAG, "Router not yet initialized");
            return false;
        }

        StreamRequestMessage deviceDescRetrievalMsg;
        StreamResponseMessage deviceDescMsg;

        try {

            deviceDescRetrievalMsg =
                    new StreamRequestMessage(UpnpRequest.Method.GET, mDevice.getIdentity().getDescriptorURL());

            // Extra headers
            UpnpHeaders headers =
                    mUpnpService.getConfiguration().getDescriptorRetrievalHeaders(mDevice.getIdentity());
            if (headers != null)
                deviceDescRetrievalMsg.getHeaders().putAll(headers);

            if (DEBUG)
                Log.i(TAG, "Sending device descriptor retrieval message: " + deviceDescRetrievalMsg);
            deviceDescMsg = mUpnpService.get().getRouter().send(deviceDescRetrievalMsg);

        } catch(IllegalArgumentException ex) {
            // UpnpRequest constructor can throw IllegalArgumentException on invalid URI
            // IllegalArgumentException can also be thrown by Apache HttpClient on blank URI in send()
            Log.w(TAG,
                    "Device descriptor retrieval failed: "
                            + mDevice.getIdentity().getDescriptorURL()
                            + ", possibly invalid URL: " + ex);
            return false;
        }

        if (deviceDescMsg == null) {
            Log.w(TAG,
                    "Device descriptor retrieval failed, no response: " + mDevice.getIdentity().getDescriptorURL()
            );
            return false;
        }

        if (deviceDescMsg.getOperation().isFailed()) {
            Log.w(TAG,
                    "Device descriptor retrieval failed: "
                            + mDevice.getIdentity().getDescriptorURL() +
                            ", "
                            + deviceDescMsg.getOperation().getResponseDetails()
            );
            return false;
        }

        if (!deviceDescMsg.isContentTypeTextUDA()) {
            Log.w(TAG,
                    "Received device descriptor without or with invalid Content-Type: "
                            + mDevice.getIdentity().getDescriptorURL());
            // We continue despite the invalid UPnP message because we can still hope to convert the content
        }

        String descriptorContent = deviceDescMsg.getBodyString();
        if (descriptorContent == null || descriptorContent.length() == 0) {
            Log.w(TAG, "Received empty device descriptor:" + mDevice.getIdentity().getDescriptorURL());
            return false;
        }

        if (DEBUG)
            Log.i(TAG, "Received root device descriptor: " + deviceDescMsg);
        describe(descriptorContent);
        return true;
    }

    private void describe(String descriptorXML) throws RouterException {
        boolean notifiedStart = false;
        RemoteDevice describedDevice = null;
        try {

            DeviceDescriptorBinder deviceDescriptorBinder =
                    mUpnpService.getConfiguration().getDeviceDescriptorBinderUDA10();

            describedDevice = deviceDescriptorBinder.describe(
                    mDevice,
                    descriptorXML
            );

            if (DEBUG)
                Log.i(TAG, "Remote device described (without services) notifying listeners: " + describedDevice);
            notifiedStart = mUpnpService.getRegistry().notifyDiscoveryStart(describedDevice);

            if (DEBUG)
                Log.i(TAG, "Hydrating described device's services: " + describedDevice);
            RemoteDevice hydratedDevice = describeServices(describedDevice);
            if (hydratedDevice == null) {
                //if(!errorsAlreadyLogged.contains(rd.getIdentity().getUdn())) {
                //    errorsAlreadyLogged.add(rd.getIdentity().getUdn());
                Log.w(TAG, "Device service description failed: " + mDevice);
                //}
                if (notifiedStart)
                    mUpnpService.getRegistry().notifyDiscoveryFailure(
                            describedDevice,
                            new DescriptorBindingException("Device service description failed: " + mDevice)
                    );
            } else {
                if (DEBUG)
                    Log.i(TAG, "Adding fully hydrated remote device to registry: " + hydratedDevice);
                // The registry will do the right thing: A new root device is going to be added, if it's
                // already present or we just received the descriptor again (because we got an embedded
                // devices' notification), it will simply update the expiration timestamp of the root
                // device.
                mUpnpService.getRegistry().addDevice(hydratedDevice);
            }

        } catch (ValidationException ex) {
            // Avoid error log spam each time device is discovered, errors are logged once per device.
            //if(!errorsAlreadyLogged.contains(rd.getIdentity().getUdn())) {
            //    errorsAlreadyLogged.add(rd.getIdentity().getUdn());
            Log.w(TAG, "Could not validate device model: " + mDevice);
            for (ValidationError validationError : ex.getErrors()) {
                Log.w(TAG, validationError.toString());
            }
            if (describedDevice != null && notifiedStart)
                mUpnpService.getRegistry().notifyDiscoveryFailure(describedDevice, ex);
            //}

        } catch (DescriptorBindingException ex) {
            Log.w(TAG, "Could not hydrate device or its services from descriptor: " + mDevice);
            Log.w(TAG, "Cause was: " + ex.toString());
            if (describedDevice != null && notifiedStart)
                mUpnpService.getRegistry().notifyDiscoveryFailure(describedDevice, ex);

        } catch (RegistrationException ex) {
            Log.w(TAG, "Adding hydrated device to registry failed: " + mDevice);
            Log.w(TAG, "Cause was: " + ex.toString());
            if (describedDevice != null && notifiedStart)
                mUpnpService.getRegistry().notifyDiscoveryFailure(describedDevice, ex);
        }
    }

    private RemoteDevice describeServices(RemoteDevice currentDevice)
            throws RouterException, DescriptorBindingException, ValidationException {

        List<RemoteService> describedServices = new ArrayList<>();
        if (currentDevice.hasServices()) {
            List<RemoteService> filteredServices = filterExclusiveServices(currentDevice.getServices());
            for (RemoteService service : filteredServices) {
                RemoteService svc = describeService(service);
                // Skip invalid services (yes, we can continue with only some services available)
                if (svc != null)
                    describedServices.add(svc);
                else
                    Log.w(TAG, "Skipping invalid service '" + service + "' of: " + currentDevice);
            }
        }

        List<RemoteDevice> describedEmbeddedDevices = new ArrayList<>();
        if (currentDevice.hasEmbeddedDevices()) {
            for (RemoteDevice embeddedDevice : currentDevice.getEmbeddedDevices()) {
                // Skip invalid embedded device
                if (embeddedDevice == null)
                    continue;
                RemoteDevice describedEmbeddedDevice = describeServices(embeddedDevice);
                // Skip invalid embedded services
                if (describedEmbeddedDevice != null)
                    describedEmbeddedDevices.add(describedEmbeddedDevice);
            }
        }

        Icon[] iconDupes = new Icon[currentDevice.getIcons().length];
        for (int i = 0; i < currentDevice.getIcons().length; i++) {
            Icon icon = currentDevice.getIcons()[i];
            iconDupes[i] = icon.deepCopy();
        }

        // Yes, we create a completely new immutable graph here
        return currentDevice.newInstance(
                currentDevice.getIdentity().getUdn(),
                currentDevice.getVersion(),
                currentDevice.getType(),
                currentDevice.getDetails(),
                iconDupes,
                currentDevice.toServiceArray(describedServices),
                describedEmbeddedDevices
        );
    }

    private RemoteService describeService(RemoteService service)
            throws RouterException, DescriptorBindingException, ValidationException {

        URL descriptorURL;
        try {
            descriptorURL = service.getDevice().normalizeURI(service.getDescriptorURI());
        }  catch(IllegalArgumentException e) {
            Log.w(TAG, "Could not normalize service descriptor URL: " + service.getDescriptorURI());
            return null;
        }

        StreamRequestMessage serviceDescRetrievalMsg = new StreamRequestMessage(UpnpRequest.Method.GET, descriptorURL);

        // Extra headers
        UpnpHeaders headers =
                mUpnpService.getConfiguration().getDescriptorRetrievalHeaders(service.getDevice().getIdentity());
        if (headers != null)
            serviceDescRetrievalMsg.getHeaders().putAll(headers);

        if (DEBUG)
            Log.i(TAG, "Sending service descriptor retrieval message: " + serviceDescRetrievalMsg);
        StreamResponseMessage serviceDescMsg = mUpnpService.get().getRouter().send(serviceDescRetrievalMsg);

        if (serviceDescMsg == null) {
            Log.w(TAG, "Could not retrieve service descriptor, no response: " + service);
            return null;
        }

        if (serviceDescMsg.getOperation().isFailed()) {
            Log.w(TAG, "Service descriptor retrieval failed: "
                    + descriptorURL
                    + ", "
                    + serviceDescMsg.getOperation().getResponseDetails());
            return null;
        }

        if (!serviceDescMsg.isContentTypeTextUDA()) {
            if (DEBUG)
                Log.i(TAG, "Received service descriptor without or with invalid Content-Type: " + descriptorURL);
            // We continue despite the invalid UPnP message because we can still hope to convert the content
        }

        String descriptorContent = serviceDescMsg.getBodyString();
        if (descriptorContent == null || descriptorContent.length() == 0) {
            Log.w(TAG, "Received empty service descriptor:" + descriptorURL);
            return null;
        }

        if (DEBUG)
            Log.i(TAG, "Received service descriptor, hydrating service model: " + serviceDescMsg);
        ServiceDescriptorBinder serviceDescriptorBinder =
                mUpnpService.getConfiguration().getServiceDescriptorBinderUDA10();

        return serviceDescriptorBinder.describe(service, descriptorContent);
    }

    private List<RemoteService> filterExclusiveServices(RemoteService[] services) {
        ServiceType[] exclusiveTypes = mUpnpService.getConfiguration().getExclusiveServiceTypes();

        if (exclusiveTypes == null || exclusiveTypes.length == 0)
            return Arrays.asList(services);

        List<RemoteService> exclusiveServices = new ArrayList<>();
        for (RemoteService discoveredService : services) {
            for (ServiceType exclusiveType : exclusiveTypes) {
                if (discoveredService.getServiceType().implementsVersion(exclusiveType)) {
                    if (DEBUG)
                        Log.i(TAG, "Including exclusive service: " + discoveredService);
                    exclusiveServices.add(discoveredService);
                } else {
                    if (DEBUG)
                        Log.i(TAG, "Excluding unwanted service: " + exclusiveType);
                }
            }
        }
        return exclusiveServices;
    }


}
