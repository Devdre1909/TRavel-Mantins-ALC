package com.dre.travelmanticsalc;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Objects;

public class DealActivity extends AppCompatActivity {

    TextInputEditText title;
    TextInputEditText price;
    TextInputEditText desc;
    Button btnUploadImage;
    ImageView imageView;

    private static final int PICTURE_RESULT = 42;

    TravelDeal deal;

    ArrayList<TravelDeal> deals;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabaseReference;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.insert_menu, menu);

        if (FirebaseUtils.isAdmin){
            menu.findItem(R.id.SaveMenuOption).setVisible(true);
            menu.findItem(R.id.deleteMenuOption).setVisible(true);
            enableEditText(true);
        } else {
            menu.findItem(R.id.SaveMenuOption).setVisible(false);
            menu.findItem(R.id.deleteMenuOption).setVisible(false);
            enableEditText(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.SaveMenuOption) {
            saveDeal();
            Toast.makeText(this, "Deal Saved!", Toast.LENGTH_LONG).show();
            clear();
            backToList();
        } else if(item.getItemId() == R.id.deleteMenuOption) {
            deleteDeal();
            Toast.makeText(this, "Deal Deleted!", Toast.LENGTH_SHORT).show();
            backToList();
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveDeal() {
        deal.setTitle(Objects.requireNonNull(title.getText()).toString());
        deal.setPrice(Objects.requireNonNull(price.getText()).toString());
        deal.setDescription(Objects.requireNonNull(desc.getText()).toString());
        String imageUrl = "";
        if(deal.getId() == null) mDatabaseReference.push().setValue(deal);
        else mDatabaseReference.child(deal.getId()).setValue(deal);
    }

    private  void deleteDeal(){
        if(deal == null) Toast.makeText(this, "Please save before deleting!", Toast.LENGTH_SHORT).show();
        else mDatabaseReference.child(deal.getId()).removeValue();
        if (deal.getImageName() != null && !deal.getImageName().isEmpty()) {
            StorageReference picRef = FirebaseUtils.mStorage.getReference().child(deal.getImageName());
            picRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Toast.makeText(DealActivity.this, "Image deleted!", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(DealActivity.this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();     
                }
            });
        }
    }

    private void backToList(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private void clear(){
        title.setText("");
        price.setText("");
        desc.setText("");
        title.requestFocus();
    }

    private void showImage(String url){
        if(url != null && !url.isEmpty()){
            int width = Resources.getSystem().getDisplayMetrics().widthPixels;
            int height = Resources.getSystem().getDisplayMetrics().heightPixels;
            Picasso.get()
                    .load(url)
                    .resize(width, height)
                    .centerCrop()
                    .into(imageView);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICTURE_RESULT){
            Uri imageUri = Objects.requireNonNull(data).getData();
            final StorageReference ref = FirebaseUtils.mStorageRef.child(Objects.requireNonNull(imageUri).getLastPathSegment());
            UploadTask uploadTask = ref.putFile(imageUri);
            Task<Uri> uriTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    // Continue with the task to get the download URL
                    return ref.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        Uri downloadUri = task.getResult();
                        String downloadURL = downloadUri.toString();
                        String pictureName = downloadUri.getPath();
                        deal.setImageUrl(downloadURL);
                        deal.setImageName(pictureName);
                        showImage(downloadURL);
                    } else {
                        Toast.makeText(DealActivity.this, "Unable to upload image", Toast.LENGTH_SHORT).show();
                        backToList();
                    }
                }
            });

        }
    }

    private void enableEditText(boolean isEnabled){
        title.setEnabled(isEnabled);
        title.clearFocus();
        desc.setEnabled(isEnabled);
        desc.clearFocus();
        price.setEnabled(isEnabled);
        price.clearFocus();
        if(isEnabled) btnUploadImage.setVisibility(View.VISIBLE);
        else btnUploadImage.setVisibility(View.GONE);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insert_deals);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        title = findViewById(R.id.titleLocation);
        desc = findViewById(R.id.desc);
        price = findViewById(R.id.price);

        imageView = findViewById(R.id.imageShow);

        Intent intent = getIntent();
        TravelDeal deal = (TravelDeal) intent.getSerializableExtra("Deal");
        if(deal==null){
            deal = new TravelDeal();
        }

        this.deal = deal;
        title.setText(deal.getTitle());
        price.setText(deal.getPrice());
        desc.setText(deal.getDescription());
        showImage(deal.getImageUrl());

        btnUploadImage = findViewById(R.id.btnUploadImage);
        btnUploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent,"Insert Picture"), PICTURE_RESULT);
            }
        });

        FirebaseUtils.openFBReference("traveldeals", this);
        mFirebaseDatabase = FirebaseUtils.mFirebaseDatabase;
        mDatabaseReference = FirebaseUtils.mDatabaseReference;
    }

}
