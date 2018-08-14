package com.example.arkadygamza.rxcamera2;

import android.annotation.TargetApi;
import android.media.Image;
import android.media.ImageReader;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

import io.reactivex.Observable;
import io.reactivex.Single;


/**
 * Saves a JPEG {@link Image} into the specified {@link File}.
 */
@TargetApi(21)
class ImageSaverRxWrapper {

    @NonNull
    public static Single<File> save(@NonNull Image image, @NonNull File file) {
        return Single.fromCallable(() -> {
            try (FileChannel output = new FileOutputStream(file).getChannel()) {
                output.write(image.getPlanes()[0].getBuffer());
                return file;
            }
            finally {
                image.close();
            }
        });
    }

    @NonNull
    public static Observable<ImageReader> createOnImageAvailableObservable(@NonNull ImageReader imageReader) {
        return Observable.create(subscriber -> {

            ImageReader.OnImageAvailableListener listener = reader -> {
                if (!subscriber.isDisposed()) {
                    subscriber.onNext(reader);
                }
            };
            imageReader.setOnImageAvailableListener(listener, null);
            subscriber.setCancellable(() -> imageReader.setOnImageAvailableListener(null, null)); //remove listener on unsubscribe
        });
    }
}
