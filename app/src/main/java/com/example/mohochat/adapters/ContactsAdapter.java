package com.example.mohochat.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mohochat.R;
import com.example.mohochat.Users;
import com.example.mohochat.models.Contact;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactViewHolder> {

    private Context context;
    private ArrayList<Contact> contactsList;

    public ContactsAdapter(Context context, ArrayList<Contact> contactsList) {
        this.context = context;
        this.contactsList = contactsList;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        Contact contact = contactsList.get(position);

        holder.contactName.setText(contact.getContactName());
        holder.contactPhone.setText(contact.getContactPhone());

        // Load contact user info
        FirebaseDatabase.getInstance().getReference()
                .child("user").child(contact.getContactUserId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Users user = snapshot.getValue(Users.class);
                        if (user != null) {
                            // Update online status indicator
                            if (user.isOnline()) {
                                holder.onlineIndicator.setVisibility(View.VISIBLE);
                            } else {
                                holder.onlineIndicator.setVisibility(View.GONE);
                            }

                            // Load profile picture
                            if (user.getProfilepic() != null && !user.getProfilepic().isEmpty()) {
                                Picasso.get().load(user.getProfilepic()).into(holder.profileImage);
                            }

                            // Update status
                            if (user.isOnline()) {
                                holder.contactStatus.setText("Online");
                                holder.contactStatus.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
                            } else {
                                holder.contactStatus.setText("Last seen recently");
                                holder.contactStatus.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, com.example.mohochat.ChatActivity.class);
            intent.putExtra("receiverId", contact.getContactUserId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return contactsList.size();
    }

    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        CircleImageView profileImage;
        TextView contactName, contactPhone, contactStatus;
        View onlineIndicator;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profileImage);
            contactName = itemView.findViewById(R.id.contactName);
            contactPhone = itemView.findViewById(R.id.contactPhone);
            contactStatus = itemView.findViewById(R.id.contactStatus);
            onlineIndicator = itemView.findViewById(R.id.onlineIndicator);
        }
    }
}