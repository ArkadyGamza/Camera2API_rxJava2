package camera;

import android.annotation.TargetApi;
import android.media.Image;
import android.media.ImageReader;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

import rx.Observable;
import rx.Single;
import rx.subscriptions.Subscriptions;

/**
 * Saves a JPEG {@link Image} into the specified {@link File}.
 */
@TargetApi(21)
class ImageSaverRxWrapper {

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
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(reader);
                }
            };
            imageReader.setOnImageAvailableListener(listener, null);
            subscriber.add(Subscriptions.create(() -> imageReader.setOnImageAvailableListener(null, null))); //remove listener on unsubscribe
        });
    }
}
