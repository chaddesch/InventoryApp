package com.example.chad.inventoryapp;

import android.content.ContentResolver;
import android.content.ContentUris;
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

import com.example.chad.inventoryapp.Data.ItemContract.ItemEntry;


/**
 * {@link ItemCursorAdapter} is an adapter for a list or grid view
 * that uses a {@link Cursor} of item data as its data source. This adapter knows
 * how to create list items for each row of item data in the {@link Cursor}.
 */

public class ItemCursorAdapter extends CursorAdapter {

    /**
     * Constructs a new {@link ItemCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    public ItemCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * This method binds the item data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current item can be set on the name TextView
     * in the list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        // Find individual views that we want to modify in the list item layout
        TextView nameTextView = (TextView) view.findViewById(R.id.name);
        TextView summaryTextView = (TextView) view.findViewById(R.id.summary);
        final TextView quantityTextView = (TextView) view.findViewById(R.id.catalog_quantity);
        Button sellButton = (Button) view.findViewById(R.id.sell_one);

        // Find the columns of item attributes that we're interested in
        int idColumnIndex = cursor.getColumnIndex(ItemEntry._ID);
        int nameColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_NAME);
        int priceColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_PRICE);
        int quantityColumnIndex = cursor.getColumnIndex(ItemEntry.COLUMN_ITEM_QUANTITY);

        // Read the item attributes from the Cursor for the current item
        final String id = cursor.getString(idColumnIndex);
        String itemName = cursor.getString(nameColumnIndex);
        String itemPrice = cursor.getString(priceColumnIndex);
        final String itemQuantity = cursor.getString(quantityColumnIndex);

        // Update the TextViews with the attributes for the current item
        nameTextView.setText(itemName);
        summaryTextView.setText(itemPrice);
        quantityTextView.setText(itemQuantity);

        // Setup OnClickListener for Sell Button
        sellButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ContentResolver resolver = v.getContext().getContentResolver();
                ContentValues values = new ContentValues();
                // Parse quantity back to an Integer so we can do math on it.
                int quantity = Integer.parseInt(itemQuantity);
                // If the quantity is 0 display a toast stating there's no inventory and return
                // Otherwise, decrement the quantity by 1
                if (quantity == 0) {
                    Toast.makeText(context.getApplicationContext(), R.string.no_inventory,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                quantity--;

                // Update the quantity of the item of the affected item in the database
                Integer itemId = Integer.parseInt(id);
                values.put(ItemEntry.COLUMN_ITEM_QUANTITY, quantity);
                Uri currentItemUri = ContentUris.withAppendedId(ItemEntry.CONTENT_URI, itemId);
                resolver.update(currentItemUri, values, null, null);

                // Set the new quantity in the quantityTextView of the affected list item
                quantityTextView.setText(Integer.toString(quantity));
            }
        });
    }
}
