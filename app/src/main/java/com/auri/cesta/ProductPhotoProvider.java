package com.auri.cesta;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

/** Da a la cámara acceso temporal únicamente al archivo que debe fotografiar. */
public final class ProductPhotoProvider extends ContentProvider {
    public static Uri uriForFile(Context context, File file) {
        return new Uri.Builder()
                .scheme("content")
                .authority(context.getPackageName() + ".photos")
                .appendPath("capture")
                .appendPath(file.getName())
                .build();
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String getType(Uri uri) {
        return "image/jpeg";
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File file = resolveFile(uri);
        int descriptorMode = ParcelFileDescriptor.parseMode(mode == null ? "r" : mode);
        return ParcelFileDescriptor.open(file, descriptorMode);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        try {
            File file = resolveFile(uri);
            String[] columns = projection == null
                    ? new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}
                    : projection;
            MatrixCursor cursor = new MatrixCursor(columns, 1);
            MatrixCursor.RowBuilder row = cursor.newRow();
            for (String column : columns) {
                if (OpenableColumns.DISPLAY_NAME.equals(column)) row.add(file.getName());
                else if (OpenableColumns.SIZE.equals(column)) row.add(file.length());
                else row.add(null);
            }
            return cursor;
        } catch (FileNotFoundException exception) {
            return new MatrixCursor(projection == null ? new String[0] : projection, 0);
        }
    }

    private File resolveFile(Uri uri) throws FileNotFoundException {
        Context context = getContext();
        List<String> segments = uri.getPathSegments();
        if (context == null || segments.size() != 2 || !"capture".equals(segments.get(0))) {
            throw new FileNotFoundException("Ruta de foto no válida");
        }
        String name = segments.get(1);
        if (!name.matches("product_[0-9_]+\\.jpg")) {
            throw new FileNotFoundException("Nombre de foto no válido");
        }
        try {
            File base = new File(context.getFilesDir(), "product_photos").getCanonicalFile();
            File target = new File(base, name).getCanonicalFile();
            if (!target.getPath().startsWith(base.getPath() + File.separator)) {
                throw new FileNotFoundException("La foto no está disponible");
            }
            return target;
        } catch (IOException exception) {
            throw new FileNotFoundException("No se pudo abrir la foto");
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("No se permite insertar");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
