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

/** Comparte únicamente listas creadas por la app, sin exponer otros archivos internos. */
public class ShareListProvider extends ContentProvider {
    public static final String MIME_TYPE = "application/vnd.auri.cesta-list+json";

    public static Uri uriForFile(Context context, File file) {
        return new Uri.Builder()
                .scheme("content")
                .authority(context.getPackageName() + ".shared")
                .appendPath("lists")
                .appendPath(file.getName())
                .build();
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String getType(Uri uri) {
        return MIME_TYPE;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("Solo se permite lectura");
        return ParcelFileDescriptor.open(resolveFile(uri), ParcelFileDescriptor.MODE_READ_ONLY);
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
        if (context == null || segments.size() != 2 || !"lists".equals(segments.get(0))) {
            throw new FileNotFoundException("Ruta de lista no válida");
        }
        String name = segments.get(1);
        if (!name.endsWith(".auri") || name.contains("/") || name.contains("\\")) {
            throw new FileNotFoundException("Nombre de lista no válido");
        }
        try {
            File base = new File(context.getCacheDir(), "shared_lists").getCanonicalFile();
            File target = new File(base, name).getCanonicalFile();
            if (!target.getPath().startsWith(base.getPath() + File.separator) || !target.isFile()) {
                throw new FileNotFoundException("La lista ya no está disponible");
            }
            return target;
        } catch (IOException exception) {
            throw new FileNotFoundException("No se pudo abrir la lista");
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Solo lectura");
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
