package com.example.bussingsw.ui.Accounts;

import static android.app.Activity.RESULT_OK;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bussingsw.R;
import com.example.bussingsw.ui.setup_account.SignIn;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AccountFragment extends Fragment {

    ShapeableImageView userProfile;
    TextInputEditText userName, userEmail, userID;
    MaterialButton logoutBtn;

    FirebaseAuth auth;
    FirebaseUser currentUser;
    GoogleSignInClient googleSignInClient;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;

    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;
    private ProgressDialog progressDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_account, container, false);

        userProfile = root.findViewById(R.id.userProfile);
        userName = root.findViewById(R.id.userNameTextInput);
        userEmail = root.findViewById(R.id.emailTextInput);
        userID = root.findViewById(R.id.uidTextInput);
        logoutBtn = root.findViewById(R.id.logoutButton);

        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Signing out...");
        progressDialog.setCancelable(false);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference().child("DUsers");

        // Initialize Google Sign-In Client
        googleSignInClient = GoogleSignIn.getClient(requireContext(),
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.client_id))
                        .requestEmail()
                        .build());


        // Check if the user is logged in and display their info
        if (currentUser != null) {
            String userId = currentUser.getUid();

            // 🔄 Load name from Realtime Database
            databaseReference.child(userId).child("name").get()
                    .addOnSuccessListener(snapshot -> {
                        String fullName = snapshot.getValue(String.class);
                        if (fullName != null && !fullName.isEmpty()) {
                            userName.setText(fullName);
                        } else {
                            userName.setText("User");
                        }
                    })
                    .addOnFailureListener(e -> {
                        userName.setText("User");
                    });

            userEmail.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "No email available");
            this.userID.setText(userId);

            // 🔄 Load base64 image from Realtime Database
            databaseReference.child(userId).child("profileImageBase64").get()
                    .addOnSuccessListener(snapshot -> {
                        String base64 = snapshot.getValue(String.class);
                        if (base64 != null && !base64.isEmpty()) {
                            loadBase64Image(base64, userProfile);
                        } else {
                            userProfile.setImageResource(R.drawable.default_user1);
                        }
                    })
                    .addOnFailureListener(e -> {
                        userProfile.setImageResource(R.drawable.default_user1);
                    });

        } else {
            userName.setText("Not logged in");
            userEmail.setText("");
            userProfile.setImageResource(R.drawable.default_user1);
        }

        userProfile.setOnClickListener(v -> showPopupMenu(v));
        logoutBtn.setOnClickListener(v -> signOut());

        return root;
    }

    private void showPopupMenu(View view) {
        // Create a PopupMenu and link it to the profile image
        PopupMenu popupMenu = new PopupMenu(getContext(), view);


        // Inflate the menu from a resource file
        MenuInflater inflater = popupMenu.getMenuInflater();
        inflater.inflate(R.menu.change_profile_menu, popupMenu.getMenu());

        // Set listener for menu items
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.change_profile) {
                // Show the profile picture in full-screen (can open new activity for viewing image)
                openFileChooser();
            }
            return true;
        });

        popupMenu.show();
    }

    // Open file chooser for picking a new image
    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    // Handle the result from the image chooser
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            // Update profile image in Realtime Database
            updateProfileImage();
        }
    }

    private void updateProfileImage() {
        if (imageUri != null) {
            try {
                // Convert image to Bitmap
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), imageUri);


                // Compress and convert to Base64
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                byte[] imageBytes = baos.toByteArray();
                String base64Image = android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT);

                // 3. Save Base64 string to Realtime Database
                String userId = currentUser.getUid();
                databaseReference.child(userId).child("profileImageBase64").setValue(base64Image)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                // 4. Show in UI immediately
                                userProfile.setImageBitmap(bitmap);

                                // Broadcast change (if used by other fragments)
                                Intent intent = new Intent("com.example.PROFILE_UPDATED");
                                requireContext().sendBroadcast(intent);

                                Toast.makeText(getContext(), "Profile Image Updated", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), "Failed to update profile image", Toast.LENGTH_SHORT).show();
                            }
                        });

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Failed to process image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadBase64Image(String base64, ImageView imageView) {
        try {
            byte[] decodedBytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            imageView.setImageBitmap(decodedBitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void signOut() {
        progressDialog.show();
        auth.signOut();
        googleSignInClient.signOut().addOnCompleteListener(getActivity(), task -> {
            progressDialog.dismiss();
            Intent intent = new Intent(getContext(), SignIn.class);
            startActivity(intent);
            getActivity().finish();
        });
    }

}