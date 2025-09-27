package com.example.mohochat.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mohochat.AddContactActivity;
import com.example.mohochat.R;
import com.example.mohochat.adapters.ContactsAdapter;
import com.example.mohochat.models.Contact;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ContactsFragment extends Fragment {

    private RecyclerView contactsRecyclerView;
    private ContactsAdapter contactsAdapter;
    private ArrayList<Contact> contactsList;
    private DatabaseReference database;
    private FirebaseAuth auth;
    private FloatingActionButton fabAddContact;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contacts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        loadContacts();
        setupFab();
    }

    private void initViews(View view) {
        contactsRecyclerView = view.findViewById(R.id.contactsRecyclerView);
        fabAddContact = view.findViewById(R.id.fabAddContact);
        database = FirebaseDatabase.getInstance().getReference();
        auth = FirebaseAuth.getInstance();
        contactsList = new ArrayList<>();
    }

    private void setupRecyclerView() {
        contactsAdapter = new ContactsAdapter(getContext(), contactsList);
        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        contactsRecyclerView.setAdapter(contactsAdapter);
    }

    private void setupFab() {
        fabAddContact.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AddContactActivity.class);
            startActivity(intent);
        });
    }

    private void loadContacts() {
        String currentUserId = auth.getCurrentUser().getUid();
        database.child("contacts").child(currentUserId)
                .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                contactsList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Contact contact = dataSnapshot.getValue(Contact.class);
                    if (contact != null && !contact.isBlocked()) {
                        contactsList.add(contact);
                    }
                }
                contactsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }
}