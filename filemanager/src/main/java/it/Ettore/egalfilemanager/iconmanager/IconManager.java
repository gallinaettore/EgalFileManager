package it.Ettore.egalfilemanager.iconmanager;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.ImageView;

import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.AppWidgetTarget;

import java.io.File;
import java.io.InputStream;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;

import org.jetbrains.annotations.NotNull;

import it.Ettore.androidutilsx.utils.FileUtils;
import it.Ettore.androidutilsx.utils.MyMath;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.fileutils.FileTypes;
import it.Ettore.egalfilemanager.iconmanager.glide.GlideApkFile;
import it.Ettore.egalfilemanager.iconmanager.glide.GlideApp;
import it.Ettore.egalfilemanager.iconmanager.glide.GlideAudioFile;



/**
 * Classe per la gestione delle icone dei files
 */
public class IconManager {


    /**
     * Restituice l'icona relativa al tipo di file
     * @param fileName Nome del file
     * @return Risorsa dell'icona scelta in base all'estenzione o al tipo mime
     */
    public static @DrawableRes int iconForFile(String fileName){
        int fileType = FileTypes.getTypeForFile(fileName);
        switch (fileType){
            case FileTypes.TYPE_TESTO:
                return R.drawable.ico_file_testo;
            case FileTypes.TYPE_AUDIO:
                return R.drawable.ico_file_audio;
            case FileTypes.TYPE_VIDEO:
                return R.drawable.ico_file_video;
            case FileTypes.TYPE_IMMAGINE:
                return R.drawable.ico_file_immagine;
            case FileTypes.TYPE_PDF:
                return R.drawable.ico_file_pdf;
            case FileTypes.TYPE_ARCHIVIO:
                return R.drawable.ico_file_zip;
            case FileTypes.TYPE_WORD:
                return R.drawable.ico_file_word;
            case FileTypes.TYPE_EXCEL:
                return R.drawable.ico_file_excel;
            case FileTypes.TYPE_APK:
                return R.drawable.ico_file_apk;
            default:
                return R.drawable.ico_file;
        }
    }



    /**
     * Restituice l'icona relativa al tipo di file
     * @param file File
     * @return Risorsa dell'icona scelta in base all'estenzione o al tipo mime.
     */
    public static @DrawableRes int iconForFile(File file){
        if(file != null) {
            return iconForFile(file.getName());
        } else {
            return R.drawable.ico_file;
        }
    }


    /**
     * Restituisce l'icona formato miniatura relativa al tipo di file (utilizzata ad esempio nella visualizzazione anteprima)
     * @param file File
     * @return Risorsa dell'icona scelta in base all'estenzione o al tipo mime
     */
    public static @DrawableRes int miniaturaForFile(File file){
        int fileType = FileTypes.getTypeForFile(file);
        switch (fileType){
            case FileTypes.TYPE_TESTO:
                return R.drawable.miniatura_file_testo;
            case FileTypes.TYPE_AUDIO:
                return R.drawable.miniatura_file_audio;
            case FileTypes.TYPE_VIDEO:
                return R.drawable.miniatura_file_video;
            case FileTypes.TYPE_IMMAGINE:
                return R.drawable.miniatura_file_immagine;
            case FileTypes.TYPE_PDF:
                return R.drawable.miniatura_file_pdf;
            case FileTypes.TYPE_ARCHIVIO:
                return R.drawable.miniatura_file_zip;
            case FileTypes.TYPE_WORD:
                return R.drawable.miniatura_file_word;
            case FileTypes.TYPE_EXCEL:
                return R.drawable.miniatura_file_excel;
            case FileTypes.TYPE_APK:
                return R.drawable.miniatura_file_apk;
            default:
                return R.drawable.miniatura_file;
        }
    }


    /**
     * Restituisce un oggetto Matrix da utilizzare per ruotare correttamente l'immagine
     * @param file File jpeg
     * @return Matrix. Null se il file non è un'immagine jpeg o se l'immagine non necessita di rotazione.
     */
    @Nullable
    public static Matrix getMatrixPerCorrettoOrientamento(File file){
        final String mime = FileUtils.getMimeType(file);
        if(!mime.equalsIgnoreCase("image/jpeg")){
            return null; //solo i jpeg possono essere analizzati
        }
        int orientation;
        try {
            final ExifInterface exif = new ExifInterface(file.getAbsolutePath());
            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        } catch (Exception ignored) {
            orientation = ExifInterface.ORIENTATION_NORMAL;
        }

        final Matrix matrix = new Matrix();
        switch (orientation){
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            default:
                return null;
        }
        return matrix;
    }


    /**
     * Restituisce l'immagine di copertina di un file audio
     * @param file File audio
     * @param context Context
     * @param maxWidthDp Lunghezza massima dell'immagine restituita espressa in dpi
     * @return Immagine di copertina. Null se il file è null o se non è possibile estrarre l'immagine.
     */
    public static Bitmap getAudioPreview(File file, @NonNull Context context, final int maxWidthDp) {
        if(file == null) return null;

        int maxWidth = (int)MyMath.dpToPx(context, maxWidthDp);
        return getAlbumArtWithMetadataRetriever(file, maxWidth);
    }


    /**
     * Restituisce l'immagine di copertina di un file audio
     * @param file File audio
     * @param maxWidth Lunghezza massima dell'immagine restituita espressa in pixel
     * @return Immagine di copertina già ridimensionata. Null se il file è null o se non è possibile estrarre l'immagine.
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private static Bitmap getAlbumArtWithMetadataRetriever(File file, final int maxWidth){
        final MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        Bitmap thumbnail = null;
        try{
            metadataRetriever.setDataSource(file.getAbsolutePath());
            final byte[] art = metadataRetriever.getEmbeddedPicture();

            if(art != null) {
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(art, 0, art.length, options);
                options.inSampleSize = calculateInSampleSize(options, maxWidth, maxWidth);

                options.inJustDecodeBounds = false;
                final Bitmap bitmap = BitmapFactory.decodeByteArray(art, 0, art.length, options);

                thumbnail = ThumbnailUtils.extractThumbnail(bitmap, maxWidth, maxWidth);
            }
        } catch (Exception ignored){
        } finally {
            try {
                metadataRetriever.release();
            } catch (Exception ignored) {}
        }
        return thumbnail;
    }


    /**
     * Calculate the largest inSampleSize value
     * @param options BitmapFactory.Options
     * @param reqWidth Lunghezza dell'immagine espressa in pixel
     * @param reqHeight Altezza dell'immagine espressa in pixel
     * @return Sample size
     */
    public static int calculateInSampleSize(@NotNull BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }



    /**
     * Ottiene un bitmap a partire da un drawable
     * @param drawable Drawable
     * @return Restituisce il bitmap corrispondente
     */
    @NonNull
    public static Bitmap getBitmapFromDrawable(@NonNull Drawable drawable) {
        if(drawable instanceof BitmapDrawable){
            return ((BitmapDrawable) drawable).getBitmap();
        } else {
            final Bitmap bmp = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            final Canvas canvas = new Canvas(bmp);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bmp;
        }
    }


    /** Mostra l'immagine utilizzando Glide
     * @param file File
     * @param imageView ImageView in cui mostrare l'immagine
     * @param width Lunghezza dell'image view in pixel
     * @param heigth Altezza dell'image view in pixel
     */
    @SuppressWarnings("unchecked")
    public static void showImageWithGlide(@NonNull File file, @NonNull ImageView imageView, int width, int heigth) {
        final float thumbScale = 0.5f;
        int roundedCorners = 5;
        final RequestOptions requestOptions = buildGlideRequestOptions(file, width, heigth, roundedCorners);
        final Object objectToLoad = buildGlideObjectToLoad(imageView.getContext(), file);
        if(objectToLoad == null) return;
        GlideApp.with(imageView.getContext().getApplicationContext())
                .load(objectToLoad)
                .thumbnail(thumbScale)
                .apply(requestOptions)
                .into(imageView);
    }


    public static void showImageOnWidgetWithGlide(Context context, AppWidgetTarget appWidgetTarget, @NonNull File file, int width, int heigth) {
        int roundedCorners = 10;
        final RequestOptions requestOptions = buildGlideRequestOptions(file, width, heigth, roundedCorners);
        final Object objectToLoad = buildGlideObjectToLoad(context, file);
        if(objectToLoad == null) return;
        GlideApp.with(context.getApplicationContext())
                .asBitmap()
                .load(objectToLoad)
                .apply(requestOptions)
                .into(appWidgetTarget);
    }


    @NotNull
    private static RequestOptions buildGlideRequestOptions(@NonNull File file, int width, int heigth, int roundedCorners) {
        final int defaultIcon = IconManager.iconForFile(file);
        return new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                //.skipMemoryCache(true)
                .override(width, heigth)
                .format(DecodeFormat.PREFER_RGB_565)
                .transform(new CenterCrop(), new RoundedCorners(roundedCorners))
                .error(defaultIcon)
                .placeholder(defaultIcon);
    }


    @Nullable
    private static Object buildGlideObjectToLoad(@Nullable Context context, @Nullable File file) {
        if(file == null || context == null) return null;
        final String mime = FileUtils.getMimeType(file);
        final String ext = FileUtils.getFileExtension(file);
        Object objectToLoad;
        if (mime.startsWith("audio/")) {
            objectToLoad = new GlideAudioFile(file);
        } else if (ext.equalsIgnoreCase("apk")){
            objectToLoad = new GlideApkFile(context, file);
        } else {
            //file immagini e file video, per gli altri file mostra l'icona predefinita
            objectToLoad = file;
        }
        return objectToLoad;
    }
}
