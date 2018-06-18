package com.jrafa.inventoryapp;

import android.Manifest;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.jrafa.inventoryapp.data.InventoryContract;

public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = EditorActivity.class.getSimpleName();
    private Uri currentProductUri;
    private static final int EXISTING_PRODUCT_LOADER = 0;
    private static final int PERMISSIONS_REQUEST_PHONE_CALL = 100;

    private EditText productNameEditText;
    private EditText quantityEditText;
    private EditText priceEditText;
    private EditText supplierNameEditText;
    private EditText supplierPhoneEditText;
    private Button supplierPhoneButton;
    private Button quantityIncreaseButton;
    private Button quantityDecreaseButton;

    private boolean itemHasChanged = false;

    private View.OnTouchListener itemTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            itemHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        Intent intent = getIntent();
        currentProductUri = intent.getData();

        if (currentProductUri == null) {
            setTitle(getString(R.string.title_insert_item));
            invalidateOptionsMenu();
        } else {
            setTitle(getString(R.string.title_edit_item));
            getLoaderManager().initLoader(EXISTING_PRODUCT_LOADER, null, this);
        }

        productNameEditText = findViewById(R.id.product_name_edit);
        quantityEditText = findViewById(R.id.product_quantity_edit);
        priceEditText = findViewById(R.id.product_price_edit);
        supplierNameEditText = findViewById(R.id.supplier_name_edit);
        supplierPhoneEditText = findViewById(R.id.supplier_phone_number_edit);

        supplierPhoneButton = findViewById(R.id.phone_button);
        quantityIncreaseButton = findViewById(R.id.quantity_increment_button);
        quantityDecreaseButton = findViewById(R.id.quantity_decrement_button);

        supplierPhoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callPhone();
            }
        });

        quantityIncreaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String quantityString = quantityEditText.getText().toString().trim();
                increaseQuantity(quantityString);
            }
        });

        quantityDecreaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String quantityString = quantityEditText.getText().toString().trim();
                decreaseQuantity(quantityString);
            }
        });

        productNameEditText.setOnTouchListener(itemTouchListener);
        quantityEditText.setOnTouchListener(itemTouchListener);
        priceEditText.setOnTouchListener(itemTouchListener);
        supplierNameEditText.setOnTouchListener(itemTouchListener);
        supplierPhoneEditText.setOnTouchListener(itemTouchListener);
        quantityDecreaseButton.setOnTouchListener(itemTouchListener);
        quantityIncreaseButton.setOnTouchListener(itemTouchListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (currentProductUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                saveItem();
                //finish();
                return true;
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;
            case android.R.id.home:
                if (!itemHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (!itemHasChanged) {
            super.onBackPressed();
            return;
        }

        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                };

        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String[] projection = {
            InventoryContract.InventoryEntry._ID,
            InventoryContract.InventoryEntry.COLUMN_INVENTORY_PRODUCT_NAME,
            InventoryContract.InventoryEntry.COLUMN_INVENTORY_QUANTITY,
            InventoryContract.InventoryEntry.COLUMN_INVENTORY_PRICE,
            InventoryContract.InventoryEntry.COLUMN_INVENTORY_SUPPLIER_NAME,
            InventoryContract.InventoryEntry.COLUMN_INVENTORY_SUPPLIER_PHONE
        };

        return new CursorLoader(this, currentProductUri, projection, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        if (cursor.moveToFirst()) {
            int productNameColumnIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_INVENTORY_PRODUCT_NAME);
            int quantityColumnIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_INVENTORY_QUANTITY);
            int priceColumnIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_INVENTORY_PRICE);
            int supplierNameColumnIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_INVENTORY_SUPPLIER_NAME);
            int supplierPhoneColumnIndex = cursor.getColumnIndex(InventoryContract.InventoryEntry.COLUMN_INVENTORY_SUPPLIER_PHONE);

            productNameEditText.setText(cursor.getString(productNameColumnIndex));
            quantityEditText.setText(cursor.getString(quantityColumnIndex));
            priceEditText.setText(cursor.getString(priceColumnIndex));
            supplierNameEditText.setText(cursor.getString(supplierNameColumnIndex));
            supplierPhoneEditText.setText(cursor.getString(supplierPhoneColumnIndex));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        productNameEditText.setText("");
        priceEditText.setText("");
        quantityEditText.setText("");
        supplierNameEditText.setText("");
        supplierPhoneEditText.setText("");
    }

    private void saveItem() {
        String productNameString = productNameEditText.getText().toString().trim();
        String quantityString = quantityEditText.getText().toString().trim();
        String priceString = priceEditText.getText().toString().trim();
        String supplierNameString = supplierNameEditText.getText().toString().trim();
        String supplierPhoneString = supplierPhoneEditText.getText().toString().trim();

        if (TextUtils.isEmpty(productNameString) ||
                TextUtils.isEmpty(quantityString) ||
                TextUtils.isEmpty(priceString) ||
                TextUtils.isEmpty(supplierNameString) ||
                TextUtils.isEmpty(supplierPhoneString)) {

            displayMessage(getString(R.string.message_fill_data));
            return;
        }

        ContentValues values = new ContentValues();
        values.put(InventoryContract.InventoryEntry.COLUMN_INVENTORY_PRODUCT_NAME, productNameString);
        values.put(InventoryContract.InventoryEntry.COLUMN_INVENTORY_SUPPLIER_NAME, supplierNameString);
        values.put(InventoryContract.InventoryEntry.COLUMN_INVENTORY_SUPPLIER_PHONE, supplierPhoneString);

        Integer quantity = 0;

        if (!TextUtils.isEmpty(quantityString)){
            quantity = Integer.parseInt(quantityString);
        }

        values.put(InventoryContract.InventoryEntry.COLUMN_INVENTORY_QUANTITY, quantity);

        Double price = 0.0;

        if (!TextUtils.isEmpty(priceString)){
            price = Double.parseDouble(priceString);
        }

        values.put(InventoryContract.InventoryEntry.COLUMN_INVENTORY_PRICE, price);


        if (currentProductUri == null) {
            Uri newUri = getContentResolver().insert(InventoryContract.InventoryEntry.CONTENT_URI, values);
            Log.i(LOG_TAG, "Insert new product.");

            String message = getString(R.string.insert_item_success);

            if (newUri == null) {
                message = getString(R.string.insert_item_failed);
            }

            displayMessage(message);

        } else {
            int rowsAffected = getContentResolver().update(currentProductUri, values, null, null);
            Log.i(LOG_TAG, "Update product data.");

            String message = getString(R.string.update_item_success);

            if (rowsAffected == 0) {
                message = getString(R.string.update_item_failed);
            }

            displayMessage(message);
        }

        finish();

    }

    private void deleteItem() {
        if (currentProductUri != null) {
            int rowsDeleted = getContentResolver().delete(currentProductUri, null, null);

            if (rowsDeleted == 0) {
                displayMessage(getString(R.string.message_delete_error));
            } else {
                displayMessage(getString(R.string.message_delete_success));
            }

        }
        finish();
    }

    public void callPhone() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, PERMISSIONS_REQUEST_PHONE_CALL);
        } else {

            String supplierPhone = supplierPhoneEditText.getText().toString().trim();
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + supplierPhone));
            startActivity(intent);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_PHONE_CALL) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                callPhone();
            } else {
                displayMessage(getString(R.string.message_phone_permission_deny));
            }
        }
    }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.message_delete_question);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                deleteItem();
            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(R.string.message_discard_changes);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void increaseQuantity(String quantityString) {
        Integer quantity = 1;

        if (!TextUtils.isEmpty(quantityString)){
            quantity = Integer.parseInt(quantityString);
            quantity++;
        }

        quantityEditText.setText(quantity.toString());
    }

    public void decreaseQuantity(String quantityString) {
        if (!TextUtils.isEmpty(quantityString)) {
            Integer quantity = Integer.parseInt(quantityString);

            if (quantity > 0) {
                quantity--;
                quantityEditText.setText(quantity.toString());
            } else {
                displayMessage(getString(R.string.message_quantity_non_negative));
            }
        }
    }

    public void displayMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
