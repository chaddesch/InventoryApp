package com.example.chad.inventoryapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.chad.inventoryapp.Data.ItemContract.ItemEntry;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Allows user to create a new item or edit an existing one.
 */

public class EditorActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Identifier for the item data loader
     */
    private static final int EXISTING_ITEM_LOADER = 0;

    /**
     * Content URI for the existing item (null if it's a new item)
     */
    private Uri mCurrentItemUri;

    /**
     * EditText field to enter the item's name
     */
    private EditText mNameEditText;

    /**
     * EditText field to enter the item's price
     */
    private EditText mPriceEditText;

    /**
     * EditText field to enter the item's quantity
     */
    private EditText mQuantityEditText;

    /**
     * EditText field to enter the item's supplier name
     */
    private EditText mSupplierNameEditText;

    /**
     * EditText field to enter the item's supplier email address
     */
    private EditText mSupplierEmailEditText;

    /**
     * ImageView field to enter the item's image
     */
    private ImageView mItemImageView;

    private static final int SELECT_IMAGE = 0;

    /**
     * Log tag for EditorActivity
     */
    private static final String LOG_TAG = EditorActivity.class.getSimpleName();


    /**
     * Boolean flag that keeps track of whether the item has been edited (true) or not (false)
     */
    private boolean mItemHasChanged = false;

    /**
     * OnTouchListener that listens for any user touches on a View, implying that they are modifying
     * the view, and we change the mItemHasChanged boolean to true.
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mItemHasChanged = true;
            return false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Examine the intent that was used to launch this activity,
        // in order to figure out if we're creating a new item or editing an existing one.
        Intent intent = getIntent();
        mCurrentItemUri = intent.getData();

        // If the intent DOES NOT contain an item content URI, then we know that we are
        // creating a new item.
        if (mCurrentItemUri == null) {
            // This is a new item, so change the app bar to say "Add an Item"
            setTitle(getString(R.string.add_item));

            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete an item that hasn't been created yet.)
            invalidateOptionsMenu();
        } else {
            // Otherwise this is an existing item, so change app bar to say "Edit Item"
            setTitle(getString(R.string.edit_item));

            // Initialize a loader to read the item data from the database
            // and display the current values in the editor
            getLoaderManager().initLoader(EXISTING_ITEM_LOADER, null, this);
        }

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_item_name);
        mPriceEditText = (EditText) findViewById(R.id.edit_item_price);
        mQuantityEditText = (EditText) findViewById(R.id.quantity_edit_text);
        mSupplierNameEditText = (EditText) findViewById(R.id.edit_supplier_name);
        mSupplierEmailEditText = (EditText) findViewById(R.id.supplier_email);
        mItemImageView = (ImageView) findViewById(R.id.item_image);

        // Setup OnTouchListeners on all the input fields, so we can determine if the user
        // has touched or modified them. This will let us know if there are unsaved changes
        // or not, if the user tries to leave the editor without saving.
        mNameEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        mSupplierNameEditText.setOnTouchListener(mTouchListener);
        mSupplierEmailEditText.setOnTouchListener(mTouchListener);
        mItemImageView.setOnTouchListener(mTouchListener);

        if (mQuantityEditText.getText().toString().isEmpty()) {
            mQuantityEditText.setText("0");
            mQuantityEditText.setText(String.valueOf(0));
        }

    }

    /**
     * Get user input from editor and save item into database.
     */
    private void saveItem() {

        // Read from input fields
        // Use trim to eliminate leading or trailing white space
        String nameString = mNameEditText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();
        String quantityString = mQuantityEditText.getText().toString().trim();
        String supplierNameString = mSupplierNameEditText.getText().toString().trim();
        String supplierEmailString = mSupplierEmailEditText.getText().toString().trim();

        // Format item image as a byte array
        mItemImageView.setDrawingCacheEnabled(true);
        mItemImageView.buildDrawingCache();
        Bitmap bitmap = mItemImageView.getDrawingCache();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] imageByteArray = byteArrayOutputStream.toByteArray();

        // Check if this is supposed to be a new item
        // and check if all the fields in the editor are blank
        if (mCurrentItemUri == null &&
                TextUtils.isEmpty(nameString) && TextUtils.isEmpty(priceString) &&
                TextUtils.isEmpty(quantityString) && TextUtils.isEmpty(supplierNameString) &&
                TextUtils.isEmpty(supplierEmailString)) {
            // Since no fields were modified, we can return early without creating a new item.
            // No need to create ContentValues and no need to do any ContentProvider operations.
            return;
        }

        // If price or item quantity are empty use zero as default.
        int price = 0;
        if (!TextUtils.isEmpty(priceString)) {
            price = Integer.parseInt(priceString);
        }
        int itemQuantity = 0;
        if (!TextUtils.isEmpty(quantityString)) {
            itemQuantity = Integer.parseInt(quantityString);
        }

        // Create a ContentValues object where column names are the keys,
        // and item attributes from the editor are the values.
        ContentValues values = new ContentValues();
        values.put(ItemEntry.COLUMN_ITEM_NAME, nameString);
        values.put(ItemEntry.COLUMN_ITEM_PRICE, price);
        values.put(ItemEntry.COLUMN_ITEM_QUANTITY, itemQuantity);
        values.put(ItemEntry.COLUMN_ITEM_SUPPLIER_NAME, supplierNameString);
        values.put(ItemEntry.COLUMN_ITEM_SUPPLIER_EMAIL, supplierEmailString);
        values.put(ItemEntry.COLUMN_ITEM_IMAGE, imageByteArray);


        // Determine if this is a new or existing item by checking if mCurrentItemUri is null or not
        if (mCurrentItemUri == null) {
            // This is a NEW item, so insert a new item into the provider,
            // returning the content URI for the new item.
            Uri newUri = getContentResolver().insert(ItemEntry.CONTENT_URI, values);

            // Show a toast message depending on whether or not the insertion was successful.
            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, R.string.save_error,
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, R.string.item_saved,
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            // Otherwise this is an EXISTING item, so update the item with content URI: mCurrentItemUri
            // and pass in the new ContentValues. Pass in null for the selection and selection args
            // because mCurrentItemUri will already identify the correct row in the database that
            // we want to modify.
            int rowsAffected = getContentResolver().update(mCurrentItemUri, values, null, null);

            // Show a toast message depending on whether or not the update was successful.
            if (rowsAffected == 0) {
                // If no rows were affected, then there was an error with the update.
                Toast.makeText(this, R.string.update_error,
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the update was successful and we can display a toast.
                Toast.makeText(this, R.string.item_updated,
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    /**
     * This method is called after invalidateOptionsMenu(), so that the
     * menu can be updated (some menu items can be hidden or made visible).
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new item, hide the "Delete" menu item.
        if (mCurrentItemUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save item to database
                saveItem();
                // Exit activity
                finish();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Pop up confirmation dialog for deletion
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the item hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!mItemHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This method is called when the back button is pressed.
     */
    @Override
    public void onBackPressed() {
        // If the item hasn't changed, continue with handling back button press
        if (!mItemHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    public void add(View v) {
        // Grab the existing quantity and 1
        int quantity = Integer.parseInt(mQuantityEditText.getText().toString());
        quantity += 1;
        mQuantityEditText.setText(String.valueOf(quantity));
        mQuantityEditText.setText(String.valueOf(quantity));
    }

    public void remove(View v) {
        // Grab the existing quantity and remove one unless the quantity is 0, in which case return
        // and show toast message
        int quantity = Integer.parseInt(mQuantityEditText.getText().toString());
        if (quantity == 0) {
            Toast.makeText(this, R.string.no_negative_quantity, Toast.LENGTH_SHORT).show();
            return;
        }
        quantity -= 1;
        mQuantityEditText.setText(String.valueOf(quantity));
        mQuantityEditText.setText(String.valueOf(quantity));
    }


    public void emailSupplier(View v) {
        // Grab the email address in EditText and convert to string and trim
        String supplierEmailString = mSupplierEmailEditText.getText().toString().trim();

        // Send an intent to email handler and prefill To: line with supplier's email address
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{supplierEmailString});

        try {
            startActivity(Intent.createChooser(emailIntent, "Send Mail Using :"));
        } catch (ActivityNotFoundException e) {
            Log.e("Error: ", "no email app");
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Since the editor shows all item attributes, define a projection that contains
        // all columns from the item table
        String[] projection = {
                ItemEntry._ID,
                ItemEntry.COLUMN_ITEM_NAME,
                ItemEntry.COLUMN_ITEM_PRICE,
                ItemEntry.COLUMN_ITEM_QUANTITY,
                ItemEntry.COLUMN_ITEM_SUPPLIER_NAME,
                ItemEntry.COLUMN_ITEM_SUPPLIER_EMAIL,
                ItemEntry.COLUMN_ITEM_IMAGE};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                mCurrentItemUri,         // Query the content URI for the current item
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {
            // Find the columns of item attributes that we're interested in
            int nameColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_NAME);
            int priceColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_PRICE);
            int quantityColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_QUANTITY);
            int supplierNameColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_SUPPLIER_NAME);
            int supplierEmailColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_SUPPLIER_EMAIL);
            int itemImageColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_IMAGE);

            // Extract out the value from the Cursor for the given column index
            String name = cursor.getString(nameColumnIndex);
            int price = cursor.getInt(priceColumnIndex);
            int quantity = cursor.getInt(quantityColumnIndex);
            String supplierName = cursor.getString(supplierNameColumnIndex);
            String supplierEmail = cursor.getString(supplierEmailColumnIndex);
            byte[] image = null;
            if (cursor.getBlob(itemImageColumnIndex) != null) {
                image = cursor.getBlob(itemImageColumnIndex);
            }


            // Update the views on the screen with the values from the database
            mNameEditText.setText(name);
            mPriceEditText.setText(Integer.toString(price));
            mQuantityEditText.setText(Integer.toString(quantity));
            mSupplierNameEditText.setText(supplierName);
            mSupplierEmailEditText.setText(supplierEmail);

            // if item image is not null run it through the bitmap factory for formatting
            if (image != null) {
                Bitmap bmp = BitmapFactory.decodeByteArray(image, 0, image.length);
                mItemImageView.setImageBitmap(Bitmap.createScaledBitmap(bmp, mItemImageView.getWidth(),
                        mItemImageView.getHeight(), false));
            }
        }


    }

    /**
     * Intent allowing the user to select an image for the item
     */
    public void selectImage(View view) {
        Intent intent;

        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
        } else {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
        }

        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        // Make sure we're working with the select image intent and not another intent
        if (requestCode == SELECT_IMAGE && resultCode == Activity.RESULT_OK) {

            // Get a Uri for the image
            if (resultData != null) {
                Uri mUri = resultData.getData();
                Log.i(LOG_TAG, "Uri: " + mUri.toString());
                mItemImageView.setImageBitmap(getBitmapFromUri(mUri));
                mItemImageView.setImageAlpha(255);
            }
        }
    }

    public Bitmap getBitmapFromUri(Uri uri) {

        if (uri == null || uri.toString().isEmpty())
            return null;

        // Get the dimensions of the View
        int targetW = mItemImageView.getWidth();
        int targetH = mItemImageView.getHeight();

        InputStream input = null;
        try {
            input = this.getContentResolver().openInputStream(uri);

            // Get the dimensions of the bitmap
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(input, null, bmOptions);
            input.close();

            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;

            // Determine how much to scale down the image
            int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            bmOptions.inPurgeable = true;

            input = this.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, bmOptions);
            input.close();
            return bitmap;

        } catch (FileNotFoundException fne) {
            Log.e(LOG_TAG, getString(R.string.image_load_failed), fne);
            return null;
        } catch (Exception e) {
            Log.e(LOG_TAG, getString(R.string.image_load_failed), e);
            return null;
        } finally {
            try {
                input.close();
            } catch (IOException ioe) {

            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        mNameEditText.setText("");
        mPriceEditText.setText("");
        mQuantityEditText.setText("");
        mSupplierNameEditText.setText("");
        mSupplierEmailEditText.setText("");
    }

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when
     *                                   the user confirms they want to discard their changes
     */
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.discard_changes_question);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Prompt the user to confirm that they want to delete this item.
     */
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_question);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the item.
                deleteItem();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the item.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the item in the database.
     */
    private void deleteItem() {
        // Only perform the delete if this is an existing item.
        if (mCurrentItemUri != null) {
            // Call the ContentResolver to delete the item at the given content URI.
            // Pass in null for the selection and selection args because the mCurrentItemUri
            // content URI already identifies the item that we want.
            int rowsDeleted = getContentResolver().delete(mCurrentItemUri, null, null);

            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, R.string.deletion_error,
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, R.string.item_deleted,
                        Toast.LENGTH_SHORT).show();
            }
        }

        // Close the activity
        finish();
    }
}
