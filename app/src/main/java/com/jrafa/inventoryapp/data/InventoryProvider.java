package com.jrafa.inventoryapp.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

public class InventoryProvider extends ContentProvider {

    public static final String LOG_TAG = InventoryProvider.class.getSimpleName();
    private static final int PRODUCTS = 100;
    private static final int PRODUCT_ID = 101;

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_INVENTORY, PRODUCTS);
        uriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_INVENTORY + "/#", PRODUCT_ID);
    }

    private InventoryDbHelper inventoryDbHelper;

    @Override
    public boolean onCreate() {
        inventoryDbHelper = new InventoryDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase db = inventoryDbHelper.getReadableDatabase();

        Cursor cursor;

        Log.i(LOG_TAG, uri.toString());

        switch (uriMatcher.match(uri)) {
            case PRODUCTS:
                cursor = db.query(InventoryContract.InventoryEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case PRODUCT_ID:
                selection = InventoryContract.InventoryEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };

                cursor = db.query(InventoryContract.InventoryEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (uriMatcher.match(uri)) {
            case PRODUCTS:
                return InventoryContract.InventoryEntry.CONTENT_LIST_TYPE;
            case PRODUCT_ID:
                return InventoryContract.InventoryEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        switch (uriMatcher.match(uri)) {
            case PRODUCTS:
                return insertItem(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase db = inventoryDbHelper.getWritableDatabase();
        int rowsDeleted;

        switch (uriMatcher.match(uri)) {
            case PRODUCTS:
                rowsDeleted = db.delete(InventoryContract.InventoryEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case PRODUCT_ID:
                selection = InventoryContract.InventoryEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                rowsDeleted = db.delete(InventoryContract.InventoryEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String selection, @Nullable String[] selectionArgs) {
        switch (uriMatcher.match(uri)) {
            case PRODUCTS:
                return updateItem(uri, contentValues, selection, selectionArgs);
            case PRODUCT_ID:
                selection = InventoryContract.InventoryEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                return updateItem(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    private Uri insertItem(Uri uri, ContentValues values) {

        String productName = values.getAsString(InventoryContract.InventoryEntry.COLUMN_INVENTORY_PRODUCT_NAME);
        checkFieldValidationIsNull(productName, "Product requires a name");

        Integer productQuantity = values.getAsInteger(InventoryContract.InventoryEntry.COLUMN_INVENTORY_QUANTITY);
        checkFieldValidationIsNullAndQuantity(productQuantity, "Product requires valid quantity");

        Double productPrice = values.getAsDouble(InventoryContract.InventoryEntry.COLUMN_INVENTORY_PRICE);
        checkFieldValidationIsNullAndPrice(productPrice, "Product requires valid price");

        String supplierName = values.getAsString(InventoryContract.InventoryEntry.COLUMN_INVENTORY_SUPPLIER_NAME);
        checkFieldValidationIsNull(supplierName, "Supplier requires a name");

        String supplierPhone = values.getAsString(InventoryContract.InventoryEntry.COLUMN_INVENTORY_SUPPLIER_PHONE);
        checkFieldValidationIsNull(supplierPhone, "Supplier requires a phone");

        SQLiteDatabase db = inventoryDbHelper.getWritableDatabase();

        long id = db.insert(InventoryContract.InventoryEntry.TABLE_NAME, null, values);

        if (id==-1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return ContentUris.withAppendedId(uri, id);
    }

    private int updateItem(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (values.containsKey(InventoryContract.InventoryEntry.COLUMN_INVENTORY_PRODUCT_NAME)) {
            String productName = values.getAsString(InventoryContract.InventoryEntry.COLUMN_INVENTORY_PRODUCT_NAME);
            if (productName == null) {
                throw new IllegalArgumentException("Product requires a name");
            }
        }

        if (values.containsKey(InventoryContract.InventoryEntry.COLUMN_INVENTORY_QUANTITY)) {
            Integer productQuantity = values.getAsInteger(InventoryContract.InventoryEntry.COLUMN_INVENTORY_QUANTITY);
            if (productQuantity == null || productQuantity<0) {
                throw new IllegalArgumentException("Product requires valid quantity");
            }
        }

        if (values.containsKey(InventoryContract.InventoryEntry.COLUMN_INVENTORY_PRICE)) {
            Double productPrice = values.getAsDouble(InventoryContract.InventoryEntry.COLUMN_INVENTORY_PRICE);
            if (productPrice == null || productPrice<0) {
                throw new IllegalArgumentException("Product requires valid price");
            }
        }

        if (values.containsKey(InventoryContract.InventoryEntry.COLUMN_INVENTORY_SUPPLIER_NAME)) {
            String supplierName = values.getAsString(InventoryContract.InventoryEntry.COLUMN_INVENTORY_SUPPLIER_NAME);
            if (supplierName == null) {
                throw new IllegalArgumentException("Supplier requires valid name");
            }
        }

        if (values.containsKey(InventoryContract.InventoryEntry.COLUMN_INVENTORY_SUPPLIER_PHONE)) {
            String supplierPhone = values.getAsString(InventoryContract.InventoryEntry.COLUMN_INVENTORY_SUPPLIER_PHONE);
            if (supplierPhone == null) {
                throw new IllegalArgumentException("Supplier requires valid phone");
            }
        }

        if (values.size() == 0) {
            return 0;
        }

        SQLiteDatabase db = inventoryDbHelper.getWritableDatabase();

        int rowsUpdated = db.update(InventoryContract.InventoryEntry.TABLE_NAME, values, selection, selectionArgs);

        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        return rowsUpdated;
    }

    public void checkFieldValidationIsNull(String value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }

    public void checkFieldValidationIsNullAndQuantity(Integer value, String message) {
        if (value == null || value<0) {
            throw new IllegalArgumentException(message);
        }
    }

    public void checkFieldValidationIsNullAndPrice(Double value, String message) {
        if (value == null || value<=0) {
            throw new IllegalArgumentException(message);
        }
    }
}
