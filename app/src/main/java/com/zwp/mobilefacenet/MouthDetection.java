package com.zwp.mobilefacenet;
//https://towardsdatascience.com/how-to-detect-mouth-open-for-face-login-84ca834dff3b
//https://medium.com/@luca_anzalone/setting-up-dlib-and-opencv-for-android-3efdbfcf9e7f

import com.zwp.mobilefacenet.mtcnn.MTCNN;

public class MouthDetection {

    private static MTCNN mtcnn;

    public static int getMouth(MTCNN got_mtcnn) {
        mtcnn = got_mtcnn;

        return 0;
    }
}
