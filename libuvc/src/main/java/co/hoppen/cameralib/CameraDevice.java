package co.hoppen.cameralib;

import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.view.Surface;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.StringUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.jiangdg.uvc.UVCCamera;

import java.util.HashMap;
import java.util.Map;

import static co.hoppen.cameralib.Instruction.MOISTURE;
import static co.hoppen.cameralib.Instruction.UNIQUE_CODE;

/**
 * Created by YangJianHui on 2022/10/8.
 */
public class CameraDevice extends Device{
    private UVCCamera uvcCamera;
    //初始设置
    private DeviceConfig deviceConfig;
    //内置摄像头设置
    private HoppenCamera.CameraConfig cameraConfig;
    private final String DEVICE_NAME = "Device_Name";

    private Surface surface;

    public void setCameraConfig(HoppenCamera.CameraConfig cameraConfig) {
        this.cameraConfig = cameraConfig;
    }

    public HoppenCamera.CameraConfig getCameraConfig() {
        return cameraConfig;
    }

    public DeviceConfig getDeviceConfig() {
        return deviceConfig;
    }


    public void getUniqueCode(){
        ThreadUtils.executeByFixed(5, new ThreadUtils.SimpleTask<byte[]>() {
            @Override
            public byte[] doInBackground() throws Throwable {
                byte[] pbuf1 = new byte[4];
                uvcCamera.jXuRead(0x02c2, 0xFE00, 4, pbuf1);
                if (pbuf1[0] == 0xff && pbuf1[1] == 0xff && pbuf1[2] == 0xff && pbuf1[3] == 0xff) {
                    return new byte[0];
                }else {
                    int len = (pbuf1[0] << 24) + (pbuf1[1] << 16) + (pbuf1[2] << 8) + pbuf1[3] - 4;
                    LogUtils.e(len);
                    //16973831
                    byte[] pbuf2 = new byte[len];
                    return pbuf2;
                }
            }

            @Override
            public void onSuccess(byte[] result) {
                uvcCamera.jXuRead(0x02c2, 0xFE00 + 4, result.length, result);
                String info = new String(result, 0, 12);
                LogUtils.e(info);
            }
        });
    }

    @Override
    void sendInstruction(Instruction instruction) {
        //异步发送指令
        if (uvcCamera!=null && instruction!=null &&cameraConfig!=null){
            if (!deviceConfig.isMcuCommunication()){
                ThreadUtils.executeByFixed(5, new ThreadUtils.SimpleTask<Map<Instruction, Object>>() {
                    @Override
                    public Map<Instruction, Object> doInBackground() throws Throwable {
                         Map<Instruction, Object> resultMap= new HashMap<>();
                            if (deviceConfig.getCommunicationType()==CommunicationType.INTERNAL_THREE_LIGHT){
                                int writeCmd = 0x82;
                                int readCmd = 0xc2;
                                int writeAddr = 0xd55b;
                                int readAddr = 0xd55c;
                                byte[] pdat = new byte[4];
                                pdat[0] = 0x0;    // 0 for write, 1 for read
                                pdat[1] = 0x78;    // slave id (same for read and write)
                                pdat[2] = 0;
                                pdat[3] = 0;
                                if (instruction== MOISTURE){
                                    pdat[0] = 0x1;    // 0 for write, 1 for read
                                    pdat[1] = 0x78;
                                    pdat[2] = 0x79;
                                    uvcCamera.jXuWrite(writeCmd, writeAddr, pdat.length, pdat);
                                    uvcCamera.jXuRead(readCmd, readAddr, pdat.length, pdat);
                                    int a = pdat[0];
                                    int b = pdat[1];
                                    int c = pdat[2];
                                    int d = a^b;
                                    if (c == d){
                                        float water = Float.parseFloat(a+ "." + b);
                                        if (water >= 0) {
                                            resultMap.put(instruction,water);
                                        }
                                    }
                                }else if (instruction==UNIQUE_CODE){
//                                    byte[] pbuf1 = new byte[4];
//                                    uvcCamera.jXuRead(0x02c2, 0xFE00, 4, pbuf1);
//                                    if (pbuf1[0] == 0xff && pbuf1[1] == 0xff && pbuf1[2] == 0xff && pbuf1[3] == 0xff) {
//                                        //nothing work
//                                    } else {
//                                        int len = (pbuf1[0] << 24) + (pbuf1[1] << 16) + (pbuf1[2] << 8) + pbuf1[3] - 4;
//                                        //LogUtils.e(len);
//                                        byte[] pbuf2 = new byte[len];
//                                        //LogUtils.e(len);
//                                        uvcCamera.jXuRead(0x02c2, 0xFE00 + 4, len, pbuf2);
//                                        String info = new String(pbuf2, 0, 12);
//                                        resultMap.put(instruction,info);
//                                    }
                                    resultMap.put(instruction,"");
                                }else {
                                    switch (instruction) {
                                        case LIGHT_CLOSE:
                                            pdat[2] = 0x10;
                                            pdat[3] = 0x00;
                                            break;
                                        case LIGHT_UV:
                                            pdat[2] = 0x13;
                                            pdat[3] = (byte) 0xff;
                                            break;
                                        case LIGHT_RGB:
                                            pdat[2] = 0x11;
                                            pdat[3] = (byte) 0xff;
                                            break;
                                        case LIGHT_POLARIZED:
                                            pdat[2] = 0x12;
                                            pdat[3] = (byte) 0xff;
                                            break;
                                        default:
                                            pdat = null;
                                            break;
                                    }
                                    if (pdat!=null){
                                        uvcCamera.jXuWrite(writeCmd, writeAddr, pdat.length, pdat);
                                    }
                                }
                            }else if (deviceConfig.getCommunicationType()==CommunicationType.INTERNAL_FIVE_LIGHT){
                                int writeCmd = 0x82;
                                int readCmd = 0xc2;
                                int writeAddr = 0xd816;
                                int readAddr = 0xd817;
                                int send = -1;
                                byte[] pdat = new byte[4];
                                pdat[0] = 0x0;    // 0 for write, 1 for read
                                pdat[1] = 0x78;    // slave id (same for read and write)
                                pdat[2] = 0;
                                pdat[3] = 0;
                                switch (instruction) {
                                    case LIGHT_CLOSE:
                                        pdat[2] = 0x10;
                                        pdat[3] = 0x00;
                                        break;
                                    case LIGHT_UV://4
                                        pdat[2] = 0x14;//0x11
                                        pdat[3] = (byte) 0xff;
                                        break;
                                    case LIGHT_RGB://2
                                        pdat[2] = 0x10;//0x10
                                        pdat[3] = (byte) 0xff;
                                        break;
                                    case LIGHT_POLARIZED://3
                                        pdat[2] = 0x12;
                                        pdat[3] = (byte) 0xff;
                                        break;
                                    case LIGHT_BALANCED_POLARIZED:
                                        pdat[2] = 0x13;
                                        pdat[3] = (byte) 0xff;
                                        break;
                                    case LIGHT_WOOD:
                                        pdat[2] = 0x11;//
                                        pdat[3] = (byte) 0xff;
                                        break;
                                    default:
                                        pdat =null;
                                        break;
                                }
                                if (pdat!=null){
                                    uvcCamera.jXuWrite(writeCmd, writeAddr, pdat.length, pdat);
                                }
                            }
                            return resultMap;
                    }

                    @Override
                    public void onSuccess(Map<Instruction, Object> result) {
                        if (result.containsKey(MOISTURE)) {
                            if (cameraConfig.getOnMoistureListener()!=null){
                                cameraConfig.getOnMoistureListener().onMoistureCallBack((Float) result.get(MOISTURE));
                            }
                        }else if (result.containsKey(UNIQUE_CODE)){
                            if (cameraConfig.getOnInfoListener()!=null){
                                //cameraConfig.getOnInfoListener().onInfoCallback(instruction, (String) result.get(UNIQUE_CODE));
                            }
                        }
                    }
                });
            }
        }
    }

    @Override
    void onConnecting(UsbDevice usbDevice) {
        try {
            String deviceName = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                deviceName = usbDevice.getProductName();
            }
            if (StringUtils.isEmpty(deviceName)){
                deviceName = SPUtils.getInstance().getString(DEVICE_NAME,"");
            }
            deviceConfig = DeviceConfig.getDeviceConfig(deviceName);
            LogUtils.e(deviceConfig);
            ControlBlock controlBlock = new ControlBlock(usbDevice);
            if (controlBlock.open()!=null){
                SPUtils.getInstance().put(DEVICE_NAME,deviceName);
                if (uvcCamera==null){
                    uvcCamera = new UVCCamera();
                    uvcCamera.setCurrentFrameFormat(cameraConfig.getFrameFormat());
                    uvcCamera.open(controlBlock);
                    int width = deviceConfig.getResolutionWidth();
                    int height = deviceConfig.getResolutionHeight();
                    if (cameraConfig.getResolutionWidth()!=0 && cameraConfig.getResolutionHeight()!=0){
                        width = cameraConfig.getResolutionWidth();
                        height = cameraConfig.getResolutionHeight();
                    }
                    uvcCamera.setPreviewSize(width,height);
                    startPreview();
                    cameraConfig.setDevicePathName(usbDevice.getDeviceName());
                    if (cameraConfig.getOnDeviceListener()!=null){
                        String productName = "";
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            productName = usbDevice.getProductName();
                        }
                        cameraConfig.getOnDeviceListener().onConnected(productName);
                    }
                }
            }
        }catch (Exception e){
        }
    }

    @Override
    void onDisconnect(UsbDevice usbDevice) {
        if (cameraConfig!=null){
            if (cameraConfig.getDevicePathName().equals(usbDevice.getDeviceName())){
                if (cameraConfig.getOnDeviceListener()!=null){
                    cameraConfig.getOnDeviceListener().onDisconnect();
                }
                closeDevice();
            }
        }
    }

    @Override
    void closeDevice() {
        try {
            LogUtils.e("cameraDevice closeDevice");
            if (uvcCamera != null) {
                uvcCamera.destroy();
                uvcCamera = null;
                surface.release();
                surface=null;
            }
        } catch (Exception e) {
            LogUtils.e(e.toString());
            e.printStackTrace();
        }
    }

    public void startPreview(){
        try {
            if (uvcCamera!=null && cameraConfig!=null){
                if (surface==null){
                    surface = new Surface(cameraConfig.getSurfaceTexture());
                }
                uvcCamera.setButtonCallback(cameraConfig.getCameraButtonListener());
                uvcCamera.setPreviewDisplay(surface);
                //****
                uvcCamera.updateCameraParams();
                uvcCamera.startPreview();
            }
        }catch (Exception e){
        }
    }

    public void stopPreview(){
        try {
            LogUtils.e("stopPreview");
            if (uvcCamera!=null && cameraConfig!=null){
                uvcCamera.setButtonCallback(null);
                uvcCamera.stopPreview();
            }
        }catch (Exception e){
            LogUtils.e(e.toString());
        }
    }
    
    public void updateSurface(SurfaceTexture surfaceTexture) {
        LogUtils.e("updateSurface");
        if (surface!=null){
            surface.release();
            surface = new Surface(surfaceTexture);
            startPreview();
        }
    }
//
//    @Override
//    public void onPageStop() {
//        stopPreview();
//    }
//
//    @Override
//    public void onPageDestroy() {
//        closeDevice();
//    }

}
