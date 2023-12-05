package com.dynatech2012.kamleonuserapp.camera;

/**
 * <p><font color="teal">
 * Edu:<br/>
 * Interface to get callback when image is analyzed
 */

public interface QRCodeFoundListener {
    void onQRCodeFound(String qrCode);
    void onQRCodeException(Exception e);
    void onQRCodeNotFound(Exception e);
}
