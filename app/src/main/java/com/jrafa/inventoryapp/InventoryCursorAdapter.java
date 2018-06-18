package com.jrafa.inventoryapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.jrafa.inventoryapp.data.InventoryContract;

public class InventoryCursorAdapter extends CursorAdapter {

    public InventoryCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {

        TextView productNameTextView = view.findViewById(R.id.product_name_text);
        TextView priceProductTextView = view.findViewById(R.id.product_price_text);
        final TextView quantityProductTextView = view.findViewById(R.id.product_quantity_text);
        Button quantityProductButton = view.findViewById(R.id.quantity_button);

        int productNameIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_INVENTORY_PRODUCT_NAME);
        int priceProductIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_INVENTORY_PRICE);
        int quantityProductIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_INVENTORY_QUANTITY);

        String productName = cursor.getString(productNameIndex);
        Double priceProduct = cursor.getDouble(priceProductIndex);
        Integer quantityProduct = cursor.getInt(quantityProductIndex);

        productNameTextView.setText(productName);
        priceProductTextView.setText(priceProduct.toString());
        quantityProductTextView.setText(quantityProduct.toString());

        int currentId = cursor.getInt(cursor.getColumnIndex(InventoryContract.InventoryEntry._ID));
        final Uri contentUri = Uri.withAppendedPath(InventoryContract.InventoryEntry.CONTENT_URI, Integer.toString(currentId));

        quantityProductButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Integer quantity = Integer.parseInt(quantityProductTextView.getText().toString().trim());
                if (quantity > 0) {
                    quantity--;

                    ContentValues values = new ContentValues();
                    values.put(InventoryContract.InventoryEntry.COLUMN_INVENTORY_QUANTITY, quantity);
                    context.getContentResolver().update(contentUri, values, null, null);
                    quantityProductTextView.setText(quantity.toString());

                } else {
                    Toast.makeText(context, R.string.message_quantity_non_negative, Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
}
