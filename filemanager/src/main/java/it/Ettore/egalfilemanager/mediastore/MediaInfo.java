package it.Ettore.egalfilemanager.mediastore;
/*
Copyright (c)2017 - Egal Net di Ettore Gallina
*/


import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.media.MediaMetadataRetriever;

import java.io.File;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;
import it.Ettore.androidutilsx.utils.MyMath;
import it.Ettore.androidutilsx.utils.MyUtils;
import it.Ettore.egalfilemanager.Costanti;
import it.Ettore.egalfilemanager.R;
import it.Ettore.egalfilemanager.fileutils.FileTypes;


/**
 *  Classe di utilità per l'estrazione di metadati dai file multimediali
 *  @author Ettore Gallina
 */
public class MediaInfo {

    /**
     *
     * @param file File da analizzare
     * @return True se è un file multimediale (immagine, video o audio)
     */
    public static boolean filesHasMediaMetadata(File file){
        if(file == null) return false;
        int type = FileTypes.getTypeForFile(file);
        return (type == FileTypes.TYPE_IMMAGINE || type == FileTypes.TYPE_VIDEO || type == FileTypes.TYPE_AUDIO || type == FileTypes.TYPE_APK);
    }



    /**
     *
     * @param context Context
     * @param file File da cui estrarre i metadati
     * @return Map con chiavi (nome metadati) e valori (dati metadati)
     */
    public static Map<String, String> getMetadata(@NonNull Context context, File file){
        if(file == null) return null;
        int type = FileTypes.getTypeForFile(file);
        switch (type){
            case FileTypes.TYPE_IMMAGINE:
                return getImageMetadata(context, file);
            case FileTypes.TYPE_AUDIO:
            case FileTypes.TYPE_VIDEO:
                return getAudioVideoMetadata(context, file);
            case FileTypes.TYPE_APK:
                return getApkMetadata(context, file);
            default:
                return null;
        }
    }



    /**
     *
     * @param context Context
     * @param file File da cui estrarre i metadati
     * @return Map con chiavi (nome metadati) e valori (dati metadati)
     */
    private static Map<String, String> getImageMetadata(@NonNull Context context, File file){
        final Map<String, String> metadati = new LinkedHashMap<>();

        //risoluzione
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        final String px = context.getString(R.string.unit_pixel);
        metadati.put(context.getString(R.string.media_risoluzione), String.format(Locale.ENGLISH, "%dx%d %s", options.outWidth, options.outHeight, px));

        //formato
        float grandezzaMpx = (float)options.outWidth * options.outHeight / 1000000;
        final NumberFormat nf = NumberFormat.getInstance(Locale.getDefault());
        nf.setMaximumFractionDigits(1);
        final String grandezzaMpxString = nf.format(grandezzaMpx);
        metadati.put(context.getString(R.string.media_formato), String.format(Locale.ENGLISH, "%s %s", grandezzaMpxString, context.getString(R.string.unit_megapixel)));

        //dimensione e path
        final String dimensione = MyMath.humanReadableByte(file.length(), MyUtils.stringResToStringArray(context, Costanti.ARRAY_BYTES_IDS));
        metadati.put(context.getString(R.string.media_dimensione), dimensione);
        metadati.put(context.getString(R.string.media_percorso), file.getAbsolutePath());

        //dati exif
        try {
            final ExifInterface exif = new ExifInterface(file.getAbsolutePath());

            //data
            String dataString = exif.getAttribute(ExifInterface.TAG_DATETIME);
            if(dataString != null) {
                try {
                    final SimpleDateFormat dataParser = new SimpleDateFormat("yyyy:MM:dd hh:mm:ss", Locale.ENGLISH);
                    final Date data = dataParser.parse(dataString);
                    final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.DEFAULT);
                    dataString = dateFormat.format(data);
                    metadati.put(context.getString(R.string.media_data), dataString);
                } catch (ParseException e) {
                    //inserisco la stringa con i dati così come è registrata sull'immagine
                    metadati.put(context.getString(R.string.media_data), dataString);
                }

            }

            //camera
            final String cameraMake = exif.getAttribute(ExifInterface.TAG_MAKE);
            if(cameraMake != null){
                metadati.put(context.getString(R.string.media_camera), cameraMake);
            }
            final String cameraModel = exif.getAttribute(ExifInterface.TAG_MODEL);
            if(cameraModel != null){
                metadati.put(context.getString(R.string.media_camera), cameraModel);
            }

            //orientamento
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
            switch (orientation){
                case ExifInterface.ORIENTATION_ROTATE_90:
                    metadati.put(context.getString(R.string.media_rotazione), "90°");
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    metadati.put(context.getString(R.string.media_rotazione), "180°");
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    metadati.put(context.getString(R.string.media_rotazione), "270°");
                    break;
            }

            //geolocalizzazione
            final String latitudineRational = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            final String latitudineRef = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
            final float latitudine = convertRationalLatLonToFloat(latitudineRational, latitudineRef);
            final String longitudineRational = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
            final String longitudineRef = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);
            final float longitudine = convertRationalLatLonToFloat(longitudineRational, longitudineRef);
            if(latitudine != 0f && longitudine != 0f){
                metadati.put(context.getString(R.string.media_latitudine), MyMath.doubleToString(latitudine));
                metadati.put(context.getString(R.string.media_longitudine), MyMath.doubleToString(longitudine));

                final Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                final List<Address> addresses = geocoder.getFromLocation(latitudine, longitudine, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
                final String address = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
                metadati.put(context.getString(R.string.media_localita), address);
            }

        } catch (Exception ignored){}
        return metadati;
    }



    /**
     *
     * @param context Context
     * @param file File da cui estrarre i metadati
     * @return Map con chiavi (nome metadati) e valori (dati metadati)
     */
    private static Map<String, String> getAudioVideoMetadata(@NonNull Context context, File file){
        final Map<String, String> metadati = new LinkedHashMap<>();
        final MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(file.getAbsolutePath());
        } catch (Exception e){
            //molto raramente può generare un'eccezione se il MediaMetadataRetriever è occupato a caricare le icone
            e.printStackTrace();
            try {
                retriever.release();
            } catch (RuntimeException ignored) {}
            return metadati;
        }

        //titolo
        final String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        if(title != null){
            metadati.put(context.getString(R.string.media_titolo), title);
        }

        //artista
        final String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        if(artist != null){
            metadati.put(context.getString(R.string.media_artista), artist);
        }

        //nome album
        final String albumName = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
        if(albumName != null){
            metadati.put(context.getString(R.string.media_album), albumName);
        }

        //genere
        String genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);
        if(genre != null){
            genre = genre.replace("(", "").replace(")", "");
            try{
                int genreCode = Integer.parseInt(genre);
                metadati.put(context.getString(R.string.media_genere), getGenreString(genreCode));
            } catch (Exception ignored){}
        }

        //duration
        final String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        if(duration != null){
            try{
                long durationLong = Long.parseLong(duration);
                metadati.put(context.getString(R.string.media_durata), MyMath.formatTimeMilliseconds(durationLong));
            } catch (Exception ignored){}

        }

        //risoluzione
        final String width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        final String height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        if(width != null && height != null){
            final String risoluzione = String.format("%sx%s %s", width, height, context.getString(R.string.unit_pixel));
            metadati.put(context.getString(R.string.media_risoluzione), risoluzione);
        }

        //bitrate
        String bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
        if(bitrate != null){
            try{
                int bitrateInt = Integer.parseInt(bitrate) / 1000;
                bitrate = String.format("%s %s", String.valueOf(bitrateInt), context.getString(R.string.unit_kbps));
                metadati.put(context.getString(R.string.media_bitrate), bitrate);
            } catch (Exception ignored){}
        }

        //frame
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            final String frame = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE);
            if(frame != null){
                metadati.put(context.getString(R.string.media_frame), frame);
            }
        }

        //rotation
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            final String rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            if(rotation != null && !rotation.equals("0")){
                metadati.put(context.getString(R.string.media_rotazione), rotation + "°");
            }
        }

        //track
        final String track = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER);
        if(track != null && !track.equals("0") && !track.equals("0/0")){
            metadati.put(context.getString(R.string.media_track), track);
        }

        //date
        String dateString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE);
        if(dateString != null){
            try {
                final SimpleDateFormat dataParser = new SimpleDateFormat("yyyy MM dd", Locale.ENGLISH);
                final Date data = dataParser.parse(dateString);
                final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
                dateString = dateFormat.format(data);
                metadati.put(context.getString(R.string.media_data), dateString);
            } catch (ParseException e) {
                //inserisco la stringa con i dati così come è registrata sul video
                metadati.put(context.getString(R.string.media_data), dateString);
            }
        }

        //year
        final String year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR);
        if(year != null && !year.equals("0")){
            metadati.put(context.getString(R.string.media_anno), year);
        }

        //dimensione e path
        final String dimensione = MyMath.humanReadableByte(file.length(), MyUtils.stringResToStringArray(context, Costanti.ARRAY_BYTES_IDS));
        metadati.put(context.getString(R.string.media_dimensione), dimensione);
        metadati.put(context.getString(R.string.media_percorso), file.getAbsolutePath());

        try {
            retriever.release();
        } catch (RuntimeException ignored) {}

        return metadati;
    }



    private static Map<String, String> getApkMetadata(@NonNull Context context, File file){
        final Map<String, String> metadati = new LinkedHashMap<>();

        final PackageManager pm = context.getPackageManager();
        final PackageInfo packageInfo = context.getPackageManager().getPackageArchiveInfo(file.getPath(), PackageManager.GET_ACTIVITIES);
        if(packageInfo != null) {
            final ApplicationInfo appInfo = packageInfo.applicationInfo;
            appInfo.sourceDir = file.getPath();
            appInfo.publicSourceDir = file.getPath();

            final String appName = (String)pm.getApplicationLabel(appInfo);
            metadati.put(context.getString(R.string.apk_nome_app), appName);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                metadati.put(context.getString(R.string.apk_min_sdk), String.valueOf(appInfo.minSdkVersion));
                metadati.put(context.getString(R.string.apk_target_sdk), String.valueOf(appInfo.targetSdkVersion));
            }
            metadati.put(context.getString(R.string.apk_nome_versione), packageInfo.versionName);
            metadati.put(context.getString(R.string.apk_codice_versione), String.valueOf(packageInfo.versionCode));

            //dimensione e path
            final String dimensione = MyMath.humanReadableByte(file.length(), MyUtils.stringResToStringArray(context, Costanti.ARRAY_BYTES_IDS));
            metadati.put(context.getString(R.string.media_dimensione), dimensione);
            metadati.put(context.getString(R.string.media_percorso), file.getAbsolutePath());
        }

        return metadati;
    }




    /**
     * Trasforma la rational string di latitudine e longitudine ricavata dai dati Exif
     *
     * @param rationalString Stringa ottenuta con ExifInterface.TAG_GPS_LATITUDE
     * @param ref Strimga ottenuta con ExifInterface.TAG_GPS_LATITUDE_REF
     * @return latitudino o longitudine espressa con un float
     */
    private static float convertRationalLatLonToFloat(String rationalString, String ref) {
        try {
            final String [] parts = rationalString.split(",");

            String [] pair;
            pair = parts[0].split("/");
            int degrees = (int) (Float.parseFloat(pair[0].trim())
                    / Float.parseFloat(pair[1].trim()));

            pair = parts[1].split("/");
            int minutes = (int) ((Float.parseFloat(pair[0].trim())
                    / Float.parseFloat(pair[1].trim())));

            pair = parts[2].split("/");
            float seconds = Float.parseFloat(pair[0].trim())
                    / Float.parseFloat(pair[1].trim());

            float result = degrees + (minutes / 60F) + (seconds / (60F * 60F));
            if ((ref.equals("S") || ref.equals("W"))) {
                return -result;
            }
            return result;
        } catch (RuntimeException ex) {
            return 0f;
        }
    }



    /**
     *
     * @param genreCode intero che specifica il codice del genere
     * @return Stringa contentente il nome del genere
     */
    private static String getGenreString(int genreCode){
        final String[] genres = {"Blues", "Classic Rock", "Country", "Dance", "Disco", "Funk", "Grunge", "Hip-Hop", "Jazz", "Metal", "New Age", "Oldies", "Other", "Pop", "R&B", "Rap",
                "Reggae", "Rock", "Techno", "Industrial", "Alternative", "Ska", "Death Metal", "Pranks", "Soundtrack", "Euro-Techno", "Ambient", "Trip-Hop", "Vocal", "Jazz+Funk",
                "Fusion", "Trance", "Classical", "Instrumental", "Acid", "House", "Game", "Sound Clip", "Gospel", "Noise", "AlternRock", "Bass", "Soul", "Punk", "Space", "Meditative",
                "Instrumental Pop", "Instrumental Rock", "Ethnic", "Gothic", "Darkwave", "Techno-Industrial", "Electronic", "Pop-Folk", "Eurodance", "Dream", "Southern Rock",
                "Comedy", "Cult", "Gangsta", "Top 40", "Christian Rap", "Pop/Funk", "Jungle", "Native American", "Cabaret", "New Wave", "Psychadelic", "Rave", "Showtunes", "Trailer",
                "Lo-Fi", "Tribal", "Acid Punk", "Acid Jazz", "Polka", "Retro", "Musical", "Rock & Roll", "Hard Rock"
        };
        return genres[genreCode];
    }
}
