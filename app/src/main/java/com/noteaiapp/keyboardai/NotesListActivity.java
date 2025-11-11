package com.noteaiapp.keyboardai;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.noteaiapp.keyboardai.Models.Label;
import com.noteaiapp.keyboardai.Models.Note;
import com.noteaiapp.keyboardai.adapter.NotesAdapter;
import com.noteaiapp.keyboardai.adapter.NotesAdapterPinned;
import com.noteaiapp.keyboardai.auth.LoginActivity;
import com.noteaiapp.keyboardai.calendar.CalendarActivity;
import com.noteaiapp.keyboardai.data.NoteRepository;
import com.noteaiapp.keyboardai.imagenote.ImageNoteActivity;
import com.noteaiapp.keyboardai.listitems.ListItemsActivity;
import com.noteaiapp.keyboardai.operationactivity.ArchivesActivity;
import com.noteaiapp.keyboardai.operationactivity.Feedback;
import com.noteaiapp.keyboardai.operationactivity.InstructionsActivity;
import com.noteaiapp.keyboardai.operationactivity.PoliciesActivity;
import com.noteaiapp.keyboardai.operationactivity.TrashActivity;
import com.noteaiapp.keyboardai.operationactivity.trashfiles.NotesRepositoryTrash;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

public class NotesListActivity extends AppCompatActivity {
    public interface FirestoreSyncCallback {
        void onSyncComplete(List<Note> syncedNotes);
        void onSyncFailed(Exception e);
    }
    private RecyclerView notesRecyclerView;/// notesRecyclerViewPinned;
    private NotesAdapter notesAdapter;
   // private NotesAdapterPinned notesAdapterPinned;
    View transparentOverlay;
    // In NotesListActivity.java, with your other class variables
// In NotesListActivity.java, with your other class variables
    Set<String> globalfolderlist;
    private FirebaseUser currentUser;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private static final String TAG = "com.noteaiapp.keyboardai";
    // --- END: ADD THESE ---

    private ActivityResultLauncher<String> requestPermissionLauncher;
    private List<Note> notesList;
    List<Note> allNotesFromDb;// allPinnedNotesFromDb;
    Toolbar toolbar;
    private NoteRepository noteRepository;
    TextView initialtext;
    FloatingActionButton fabAddNote;
    LinearLayout option_text_layout, option_drawings_layout,option_list_layout,option_image_layout;
    private DrawerLayout drawerLayout;
    ItemTouchHelper itemTouchHelper;//itemTouchHelperPinned;
    public static final String EXTRA_FOLDER_NAME = "FOLDER_NAME";

    private SearchView searchView;
    // Add these at the top of NotesListActivity.java
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private Uri cameraImageUri; // To store the URI for the photo taken by the camera
    static public List<Note> allNotes;// pinnedNotes;
    private ActionMode actionMode;
    private Note selectedNote;
    //TextView pinnedNotesHeader;
    public static final String LIST_NOTE_PREFIX = "[LIST_NOTE_START]";
    private LinearLayout optionsLayout;
    NotesRepositoryTrash notesRepositoryTrash;
    private boolean isOptionsVisible = false;
    ImageButton shuffle,themeColor;
    NavigationView navigationView;
    private enum DriveAction { BACKUP, RESTORE }

    private BroadcastReceiver noteUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Call the refresh logic here
            loadNotesFromDatabase();
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notes_list_main);
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        globalfolderlist = new HashSet<>();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        // --- END: INITIALIZE FIREBASE ---

        if (currentUser == null) {
            Toast.makeText(this, "No user logged in. Redirecting to login.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(NotesListActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return; // Stop executing onCreate
        }
        getWindow().setAllowEnterTransitionOverlap(false);
        getWindow().setAllowReturnTransitionOverlap(false);
        transparentOverlay = findViewById(R.id.transparent_overlay);
        transparentOverlay.setOnClickListener(v -> {
            if (isOptionsVisible) {
                hideOptions();
            }
        });
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {    if (isGranted) {
            // Permission is granted. Now, we can safely launch the camera.
            launchCamera();
        } else {
            Toast.makeText(this, "Camera permission is required to take photos.", Toast.LENGTH_LONG).show();
        }
        });
// Launcher for picking an image from the gallery
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            // The image was selected from the gallery. Now, save a copy and launch ImageNoteActivity.
                            String imagePath = saveImageToAppStorage(selectedImageUri);
                            if (imagePath != null) {
                                launchImageNoteActivity(imagePath);
                            } else {
                                Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
        );

// Launcher for taking a photo with the camera
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                isSuccess -> {
                    if (isSuccess && cameraImageUri != null) {
                        // The photo was taken successfully. The URI is in cameraImageUri.
                        // Now, save a copy and launch ImageNoteActivity.
                        String imagePath = saveImageToAppStorage(cameraImageUri);
                        if (imagePath != null) {
                            launchImageNoteActivity(imagePath);
                        } else {
                            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
        allNotesFromDb = new ArrayList<>();
        //allPinnedNotesFromDb = new ArrayList<>();
        noteRepository = new NoteRepository(this);
        notesRepositoryTrash = new NotesRepositoryTrash(this);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        option_list_layout = findViewById(R.id.option_list_layout);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        initialtext = findViewById(R.id.initialtext);
        option_drawings_layout = findViewById(R.id.option_drawings_layout);
        themeColor = findViewById(R.id.themeColor);
        option_text_layout = findViewById(R.id.option_text_layout);
        shuffle = findViewById(R.id.shuffle);
        //pinnedNotesHeader = findViewById(R.id.pinnedNotesHeader);
        searchView = findViewById(R.id.search_view);
        option_image_layout = findViewById(R.id.option_image_layout);
        fabAddNote = findViewById(R.id.fabAddNote);
        notesRecyclerView = findViewById(R.id.notesRecyclerView);
        //notesRecyclerViewPinned = findViewById(R.id.notesRecyclerViewPinned);
        //notesRecyclerViewPinned.setLayoutManager(new LinearLayoutManager(this));
        notesRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadAndApplyBackgroundColor();
        final Animation slideUpAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up);
        final Animation slideDownAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down);

        optionsLayout = findViewById(R.id.options_layout);
        TextView optionText = findViewById(R.id.option_text);
        TextView optionDrawing = findViewById(R.id.option_drawings);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);


        toggle.syncState();
        //loadFoldersToDrawer();
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.new_folder) {
                // 1. Handle the "New folder" action
                showNewFolderDialog();
            }
            else if (item.getGroupId() == R.id.folders_group) {
                // Logic to filter your notes list by this folder's name/ID
                String folderName = item.getTitle().toString();

                Intent intent = new Intent(this, FolderNotesActivity.class);
                intent.putExtra(EXTRA_FOLDER_NAME, folderName);
                startActivity(intent);

                Toast.makeText(this, "Loading notes from folder: " + folderName, Toast.LENGTH_SHORT).show();
                // TODO: Implement actual data filtering logic here
            }else if (id == R.id.nav_instructions) {
                startActivity(new Intent(this, InstructionsActivity.class));
            } else if (id == R.id.nav_trash) {
                startActivity(new Intent(this, TrashActivity.class));
            } else if (id == R.id.nav_policies) {
                startActivity(new Intent(this, PoliciesActivity.class));
            } else if (id == R.id.nav_feedback) {
                startActivity(new Intent(this, Feedback.class));
            }
            else if(id == R.id.option_logout){
                FirebaseAuth.getInstance().signOut();
                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build();
                GoogleSignIn.getClient(this, gso).signOut();
                Intent intent = new Intent(NotesListActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
            else if(id == R.id.calendar_option){
                Intent intent = new Intent(NotesListActivity.this, CalendarActivity.class);
                startActivity(intent);
            }
         else if (id == R.id.nav_archive) {
             startActivity(new Intent(this, ArchivesActivity.class));
        }
        drawerLayout.closeDrawers();
            return true;
        });
        shuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSortDialog();
            }
        });
        themeColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showThemeColorDialog();
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // This is still here for when the user hits enter on the keyboard
                filterNotes(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Call filterNotes here to enable real-time searching
                filterNotes(newText);
                return true;
            }
        });

        fabAddNote.setOnClickListener(v -> {
            if (isOptionsVisible) {
                optionsLayout.setVisibility(View.VISIBLE);
                optionsLayout.startAnimation(slideUpAnimation);
                hideOptions();
            } else {
                optionsLayout.startAnimation(slideDownAnimation);
                slideDownAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        optionsLayout.setVisibility(View.GONE);
                        optionsLayout.clearAnimation();
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                showOptions();
            }
        });

        optionText.setOnClickListener(v -> {
            Intent intent = new Intent(NotesListActivity.this, Notepad.class);
            startActivity(intent);
            hideOptions();
        });

        optionDrawing.setOnClickListener(v -> {
            Intent intent = new Intent(NotesListActivity.this, DrawingActivity.class);
            startActivity(intent);
            hideOptions();
        });
        option_list_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(NotesListActivity.this, ListItemsActivity.class);
                startActivity(intent);
                hideOptions();
            }
        });
        option_drawings_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NotesListActivity.this, DrawingActivity.class);
                startActivity(intent);
                hideOptions();
            }
        });
        option_text_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NotesListActivity.this, Notepad.class);
                startActivity(intent);
                hideOptions();
            }
        });
        option_image_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Intent intent = new Intent(NotesListActivity.this, ImageNoteActivity.class);
                //startActivity(intent);
                hideOptions();

                final CharSequence[] options = {getApplicationContext().getString(R.string.take_photo), getApplicationContext().getString(R.string.choose_from_gallery), getApplicationContext().getString(R.string.cancel_text)};
                AlertDialog.Builder builder = new AlertDialog.Builder(NotesListActivity.this);
                builder.setTitle(getApplicationContext().getString(R.string.add_image));

                builder.setItems(options, (dialog, item) -> {
                    if (options[item].equals("Take Photo")) {
                        // Check if the CAMERA permission has already been granted.
                        if (ContextCompat.checkSelfPermission(
                                NotesListActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            // If permission is already granted, launch the camera directly.
                            launchCamera();
                        } else {
                            // If permission is not granted, request it.
                            // The result will be handled by the 'requestPermissionLauncher' you defined in onCreate.
                            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
                        }
                        // Create a file to store the camera image
                        /*
                        File imageFile = null;
                        try {
                            imageFile = createImageFile();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        if (imageFile != null) {
                            // Get a content URI for the file using FileProvider
                            cameraImageUri = FileProvider.getUriForFile(
                                    NotesListActivity.this,
                                    "com.noteaiapp.keyboardai.fileprovider", // Make sure this matches your manifest
                                    imageFile
                            );
                            // Launch the camera
                            cameraLauncher.launch(cameraImageUri);
                        } else {
                            Toast.makeText(NotesListActivity.this, "Could not create image file", Toast.LENGTH_SHORT).show();
                        }
*/
                    } else if (options[item].equals("Choose from Gallery")) {
                        // Launch the gallery
                        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        galleryLauncher.launch(intent);

                    } else if (options[item].equals("Cancel")) {
                        dialog.dismiss();
                    }
                });
                builder.show();
            }
        });

        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        notesRecyclerView.setLayoutManager(layoutManager);
        notesList = new ArrayList<>();
        allNotes = new ArrayList<>();
       // pinnedNotes = new ArrayList<>();

        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
                return makeMovementFlags(dragFlags, 0);
            }
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                notesAdapter.onItemMove(fromPosition, toPosition);
                return true;
            }
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            }
            @Override
            public void onMoved(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, int fromPos, RecyclerView.ViewHolder target, int toPos, int x, int y) {
                super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y);
                notesAdapter.onItemsMoved();
            }
            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                    notesAdapter.onItemsMoved();

                } else if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    if (actionMode == null) {
                        actionMode = startSupportActionMode(actionModeCallback);
                    }
                    int position = viewHolder.getAdapterPosition();

                    if (position != RecyclerView.NO_POSITION) {
                        selectedNote = notesList.get(position);
                        //selectedNote.setSelected(true);
                    }
                }


            }

            @Override
            public boolean isLongPressDragEnabled() {
                // Enable long press drag
                return false;
            }
        };

        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(notesRecyclerView);

        notesAdapter = new NotesAdapter(this, notesList, new NotesAdapter.OnNoteClickListener() {
            @Override
            public void onNoteClick(Note note, View sharedView) {
                if (actionMode != null) {
                    actionMode.finish();
                    return;
                }
                Intent intent;
                String noteContent = note.getContent();
                boolean isListNote = noteContent != null && noteContent.startsWith(LIST_NOTE_PREFIX);
                if(isListNote){
                    intent = new Intent(NotesListActivity.this, ListItemsActivity.class);
                    Log.d("com.noteaiapp.keyboardai","list note intent called");
                }
                else if(note.getFontColor() != null && !note.getFontColor().isEmpty() && note.getFontColor().equalsIgnoreCase("imagenote")){
                    intent = new Intent(NotesListActivity.this, ImageNoteActivity.class);
                    Log.d("com.noteaiapp.keyboardai","image note intent intent called");
                }
                else if (note.getContent() != null && !note.getContent().isEmpty()) {
                    intent = new Intent(NotesListActivity.this, Notepad.class);
                    Log.d("com.noteaiapp.keyboardai","notepad intent called");
                } else if (note.getImagePath() != null && note.getImagePath().length() > 0) {
                    intent = new Intent(NotesListActivity.this, DrawingActivity.class);
                    Log.d("com.noteaiapp.keyboardai","DrawingActivity intent called");
                } else {
                    intent = new Intent(NotesListActivity.this, Notepad.class);
                }
                intent.putExtra("note_id", note.getId());
                intent.putExtra("note_title", note.getTitle());
                intent.putExtra("note_content", note.getContent());
                intent.putExtra("note_date", note.getDate());
                intent.putExtra("note_color", note.getColor());
                intent.putExtra("note_image_path",note.getImagePath());
                intent.putExtra("note_font_size",note.getUserFirebaseId());
                Log.d(TAG, "note_id: "+note.getId()+" note_title: "+note.getTitle()+" note_content: "+note.getContent()+" note_date: "+note.getDate()+" note_color: "+note.getColor()+" note_image_path: "+note.getImagePath()+" note_font_size: "+note.getUserFirebaseId()+" note_font_color: "+note.getFontColor());
                String transitionName = ViewCompat.getTransitionName(sharedView);
                if (transitionName != null) {
                    intent.putExtra("TRANSITION_NAME", transitionName);
                    ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(
                            NotesListActivity.this,
                            sharedView,
                            transitionName
                    );
                    startActivity(intent, options.toBundle());
                } else {
                    startActivity(intent);
                }

            }
        }, new NotesAdapter.OnNoteLongClickListener() {
            @Override
            public void onNoteLongClick(View view, Note note, View sharedView) {
                if (actionMode == null) {
                    selectedNote = note;
                    actionMode = startSupportActionMode(actionModeCallback);
                }
            }
        }, itemTouchHelper,notesRecyclerView);
        notesRecyclerView.setAdapter(notesAdapter);
        loadFoldersToDrawer();
      /*
        ItemTouchHelper.Callback callbackPinned = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
                return makeMovementFlags(dragFlags, 0);
            }
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                notesAdapterPinned.onItemMove(fromPosition, toPosition);
                return true;
            }
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            }
            @Override
            public void onMoved(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, int fromPos, RecyclerView.ViewHolder target, int toPos, int x, int y) {
                super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y);
                notesAdapterPinned.onItemsMoved();
            }
            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                    notesAdapterPinned.onItemsMoved();
                } else if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    if (actionMode == null) {
                        actionMode = startSupportActionMode(actionModeCallback);
                    }
                    int position = viewHolder.getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        selectedNote = pinnedNotes.get(position);
                        //selectedNote.setSelected(true);
                    }
                }
            }
            @Override
            public boolean isLongPressDragEnabled() {
                // Enable long press drag
                return false;
            }
        };

        itemTouchHelperPinned = new ItemTouchHelper(callbackPinned);
        itemTouchHelperPinned.attachToRecyclerView(notesRecyclerViewPinned);

        notesAdapterPinned = new NotesAdapterPinned(this, pinnedNotes, new NotesAdapterPinned.OnNoteClickListener() {
            @Override
            public void onNoteClick(Note note, View sharedView) {
                if (actionMode != null) {
                    actionMode.finish();
                    return;
                }
                Intent intent;
                String noteContent = note.getContent();
                boolean isListNote = noteContent != null && noteContent.startsWith(LIST_NOTE_PREFIX);
                if(isListNote){
                    intent = new Intent(NotesListActivity.this, ListItemsActivity.class);
                }
                else if (note.getContent() != null && !note.getContent().isEmpty()) {
                    intent = new Intent(NotesListActivity.this, Notepad.class);
                } else if (note.getImagePath() != null && note.getImagePath().length() > 0) {
                    intent = new Intent(NotesListActivity.this, DrawingActivity.class);
                } else {
                    intent = new Intent(NotesListActivity.this, Notepad.class);
                }
                intent.putExtra("note_id", note.getId());
                intent.putExtra("note_title", note.getTitle());
                intent.putExtra("note_content", note.getContent());
                intent.putExtra("note_date", note.getDate());
                intent.putExtra("note_color", note.getColor());
                intent.putExtra("note_image_path",note.getImagePath());

                String transitionName = ViewCompat.getTransitionName(sharedView);
                if (transitionName != null) {
                    intent.putExtra("TRANSITION_NAME", transitionName);
                    ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(
                            NotesListActivity.this,
                            sharedView,
                            transitionName
                    );
                    startActivity(intent, options.toBundle());
                } else {
                    startActivity(intent);
                }
            }
        }, new NotesAdapterPinned.OnNoteLongClickListener() {
            @Override
            public void onNoteLongClick(View view, Note note, View sharedView) {
                if (actionMode == null) {
                    selectedNote = note;
                    actionMode = startSupportActionMode(actionModeCallback);
                }
            }
        }, itemTouchHelperPinned,notesRecyclerViewPinned);
        notesRecyclerViewPinned.setAdapter(notesAdapterPinned);*/
        updatePinnedSectionVisibility();

        drawerLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isOptionsVisible && event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (!isTouchInsideView(optionsLayout, event)) {
                        hideOptions(slideDownAnimation);
                        return true;
                    }
                }
                return false;
            }
        });

    }
    private void launchCamera() {
        File imageFile = null;
        try {
            imageFile = createImageFile();
        } catch (IOException e) {
            // Log the error or show a more detailed toast
            Toast.makeText(this, "Error creating image file: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return; // Stop if the file can't be created
        }

        if (imageFile != null) {
            // Get a content URI for the file using FileProvider
            cameraImageUri = FileProvider.getUriForFile(
                    NotesListActivity.this,
                    "com.noteaiapp.keyboardai.fileprovider", // Make sure this matches your manifest
                    imageFile
            );
            // Launch the camera now that we know we have permission
            cameraLauncher.launch(cameraImageUri);
        } else {
            Toast.makeText(NotesListActivity.this, "Could not create image file", Toast.LENGTH_SHORT).show();
        }
    }
// Add this entire method inside your NotesListActivity.java class

    private void showNewFolderDialog() {
        // 1. Create an AlertDialog Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.new_folder_text);

        // 2. Inflate a custom layout containing an EditText
        //    We'll create this layout file in the next step.
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_new_folder, null);
        final EditText folderNameEditText = dialogView.findViewById(R.id.folder_name_edit_text);
        builder.setView(dialogView);

        // 3. Set up the dialog buttons ("Create" and "Cancel")
        builder.setPositiveButton(R.string.create_positive_button, (dialog, which) -> {
            // This code executes when the user clicks "Create"
            String folderName = folderNameEditText.getText().toString().trim();

            // Validate the input
            if (folderName.isEmpty()) {
                Toast.makeText(this, R.string.folder_name_empty, Toast.LENGTH_SHORT).show();
            } else if(folderName.equalsIgnoreCase("archived")){
                Toast.makeText(this, R.string.folder_name_archived, Toast.LENGTH_SHORT).show();
            }
            // check if the folder name contains only numbers, alphabets and space
            else if (!folderName.matches("^[a-zA-Z0-9 ]+$")) {
                Toast.makeText(this, R.string.no_alpha, Toast.LENGTH_SHORT).show();
            }
            else {
                // Save the folder and refresh the navigation drawer
                saveFolder(folderName.trim());
                Toast.makeText(this, "Folder '" + folderName + "' created", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton(R.string.cancel_text, (dialog, which) -> {
            // This code executes when the user clicks "Cancel"
            dialog.dismiss();
        });

        // 4. Create and show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }
// Add these methods inside your NotesListActivity.java class

    private void saveFolder(String folderName) {
        // Get the existing set of folders from SharedPreferences
        /*SharedPreferences prefs = getSharedPreferences("notes_app_folders", MODE_PRIVATE);
        Set<String> folders = new HashSet<>(prefs.getStringSet("folder_set", new HashSet<>()));

        // Add the new folder and save the updated set
        folders.add(folderName);
        prefs.edit().putStringSet("folder_set", folders).apply();*/
        db.collection("folder").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Set<String> cloudFolders = new HashSet<>();
                    if (documentSnapshot.exists()) {
                        List<String> folderArray = (List<String>) documentSnapshot.get("array");
                        if (folderArray != null) {
                            cloudFolders.addAll(folderArray);
                        }

                    }
                    cloudFolders.add(folderName);
                    globalfolderlist = cloudFolders;
                    // Now, update the UI with the fresh data
                    saveFolderToFirebase(globalfolderlist);

                    updateDrawerMenu(globalfolderlist);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error fetching folders from Firestore. Using local cache.", e);
                    // On failure (e.g., offline), just use whatever is in the cache
                    //loadFoldersToDrawer();
                });
        // Refresh the navigation drawer to show the new folder
        //loadFoldersToDrawer();
    }
    public void saveFolderToFirebase(Set<String> folders){
        Map<String, Object> folderData = new HashMap<>();
        folderData.put("array", new ArrayList<>(folders));

        // Save the entire list to the user's folder document
        db.collection("folder").document(currentUser.getUid())
                .set(folderData)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Folder list successfully synced to Firestore."))
                .addOnFailureListener(e -> Log.w(TAG, "Error syncing folder list to Firestore.", e));
    }
    // In NotesListActivity.java
    public void loadFoldersToDrawer(){
        db.collection("folder").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Set<String> cloudFolders = new HashSet<>();
                    if (documentSnapshot.exists()) {
                        List<String> folderArray = (List<String>) documentSnapshot.get("array");
                        if (folderArray != null) {
                            cloudFolders.addAll(folderArray);
                        }
                        globalfolderlist = cloudFolders;
                    }
                    updateDrawerMenu(cloudFolders);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error fetching folders from Firestore. Using local cache.", e);
                    // On failure (e.g., offline), just use whatever is in the cache
                    //loadFoldersToDrawer();
                });
    }
    public void updateDrawerMenu(Set<String> folders){
        Menu menu = navigationView.getMenu();
        SubMenu foldersSubMenu = menu.findItem(R.id.folders_group_item).getSubMenu();
        foldersSubMenu.clear(); // Clear existing folders to prevent duplicates

        // Add the static "New folder" button first
        foldersSubMenu.add(R.id.folders_group, R.id.new_folder, Menu.NONE, "New folder")
                .setIcon(R.drawable.baseline_add_24);

        if (folders != null) {
            for (String folderName : folders) {
                MenuItem folderItem = foldersSubMenu.add(R.id.folders_group, Menu.NONE, 0, folderName)
                        .setIcon(R.drawable.baseline_folder_24);

                // Set a regular click listener to open the folder
                folderItem.setOnMenuItemClickListener(item -> {
                    // TODO: Implement logic to show notes for this folder
                    Toast.makeText(this, "Opening folder: " + item.getTitle(), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(NotesListActivity.this, FolderNotesActivity.class);
                    intent.putExtra(EXTRA_FOLDER_NAME, item.getTitle().toString());
                    startActivity(intent);
                    drawerLayout.closeDrawers();
                    return true;
                });

                // Set a long-click listener to trigger the delete dialog
                View actionView = new View(this); // Create a dummy view
                actionView.setOnLongClickListener(v -> {
                    showDeleteFolderDialog(folderName);
                    return true;
                });
                folderItem.setActionView(actionView);

                // --- END OF NEW PART ---
            }
        }
    }
// Add this method to NotesListActivity.java

    private void showDeleteFolderDialog(String folderName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Folder: '" + folderName + "'")
                .setMessage(R.string.folder_remove_desc)

                // Button 1: Delete folder AND all notes within it
                .setPositiveButton("Delete Everything", (dialog, which) -> {
                    // TODO: Implement logic to find and delete all notes in this folder
                     noteRepository.deleteNotesByFolder(folderName);

                    // Then, delete the folder itself
                    deleteFolderAndRefresh(folderName);
                    Toast.makeText(this, R.string.folder_deleted_toast, Toast.LENGTH_SHORT).show();
                })

                // Button 2: Remove folder but KEEP the notes (move them to uncategorized)
                .setNegativeButton(R.string.keep_notes_folder, (dialog, which) -> {
                    new Thread(() -> {
                        noteRepository.unassignFolderFromNotes(folderName);

                        // 2. After the database is updated, refresh the UI on the main thread.
                        runOnUiThread(() -> {
                            // 3. Delete the folder from SharedPreferences and refresh the drawer.
                            deleteFolderAndRefresh(folderName);

                            // 4. THIS IS THE FIX: Refresh the main RecyclerView to show the unassigned notes.
                            loadNotesFromDatabase();

                            Toast.makeText(this, R.string.notes_kept, Toast.LENGTH_SHORT).show();
                        });
                    }).start();
                })

                // Button 3: Cancel the operation
                .setNeutralButton(R.string.cancel_text, (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }
// Add this method to NotesListActivity.java

    private void deleteFolderAndRefresh(String folderName) {
        globalfolderlist.remove(folderName);
        // Refresh the navigation drawer to reflect the deletion
        saveFolderToFirebase(globalfolderlist);
        updateDrawerMenu(globalfolderlist);
    }


    private int getCurrentBackgroundColor() {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getInt(BACKGROUND_COLOR_KEY, DEFAULT_BACKGROUND_COLOR);
    }
    private void showThemeColorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Background Color");

        // 1. Create the custom view layout for the dialog
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        // Use density-independent units (dp) for padding via multiplication
        int padding = (int) (getResources().getDisplayMetrics().density * 16);
        dialogLayout.setPadding(padding * 2, padding * 2, padding * 2, padding * 2);

        // --- Fixed Major Colors ---
        final int COLOR_BLACK = Color.BLACK;
        final int COLOR_WHITE = Color.WHITE;
        final int COLOR_BROWN = Color.rgb(139, 69, 19);
        final int COLOR_MAROON = Color.rgb(128, 0, 0);
        final int[] fixedColors = {COLOR_BLACK, COLOR_WHITE, COLOR_BROWN, COLOR_MAROON};

        // 2. Add a horizontal layout for major colors
        LinearLayout majorColorsLayout = new LinearLayout(this);
        majorColorsLayout.setOrientation(LinearLayout.HORIZONTAL);
        majorColorsLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        majorColorsLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        int colorSize = (int) (getResources().getDisplayMetrics().density * 40);
        int colorMargin = (int) (getResources().getDisplayMetrics().density * 12);

        for (int color : fixedColors) {
            View colorView = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(colorSize, colorSize);
            params.setMargins(colorMargin, colorMargin / 2, colorMargin, colorMargin / 2);
            colorView.setLayoutParams(params);

            colorView.setBackgroundColor(color);
            colorView.setTag(color); // Store the color in the tag for retrieval in the listener

            // Add a temporary border for white color visibility against a white dialog background
            if (color == Color.WHITE) {
                // A simple trick to show a slight border using padding/background if custom drawables are unavailable
                colorView.setPadding(1, 1, 1, 1);
                // Cannot easily draw border without custom drawable or new view, rely on visual.
            }

            majorColorsLayout.addView(colorView);
        }

        dialogLayout.addView(majorColorsLayout);

        // Add a separator space
        View separator = new View(this);
        LinearLayout.LayoutParams separatorParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                padding / 2
        );
        separator.setLayoutParams(separatorParams);
        dialogLayout.addView(separator);

        // 3. Add a TextView for the color preview
        TextView colorPreview = new TextView(this);
        colorPreview.setText(R.string.sliding_text_dialog);
        colorPreview.setTextColor(Color.BLACK); // Ensure text visibility
        colorPreview.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        colorPreview.setPadding(padding, padding, padding, padding);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (getResources().getDisplayMetrics().density * 60) // Slightly smaller height
        );
        previewParams.setMargins(0, 0, 0, padding); // 16dp margin bottom
        colorPreview.setLayoutParams(previewParams);
        colorPreview.setBackgroundColor(getCurrentBackgroundColor());
        dialogLayout.addView(colorPreview);


        // 4. Add a SeekBar for Hue selection (0 to 360 degrees)
        SeekBar hueSeekBar = new SeekBar(this);
        // Max progress of 360 to represent 360 degrees of hue
        hueSeekBar.setMax(360);
        // Set initial progress to current background color's hue
        float[] hsv = new float[3];
        Color.colorToHSV(getCurrentBackgroundColor(), hsv);
        hueSeekBar.setProgress((int) hsv[0]);

        LinearLayout.LayoutParams seekBarParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        hueSeekBar.setLayoutParams(seekBarParams);
        dialogLayout.addView(hueSeekBar);


        // 5. Build the dialog
        builder.setView(dialogLayout);

        // Add a CLOSE button that also resets the color if the user closed it mid-drag without stopping the touch
        builder.setNeutralButton(R.string.close_dialog, (dialog, which) -> {
            dialog.dismiss();
            // Re-apply the last permanently saved color
            loadAndApplyBackgroundColor();
        });

        final AlertDialog dialog = builder.create();

        // 6. Set click listeners for the fixed color views now that 'dialog' is defined
        for (int i = 0; i < majorColorsLayout.getChildCount(); i++) {
            View colorView = majorColorsLayout.getChildAt(i);
            colorView.setOnClickListener(v -> {
                int selectedColor = (int) v.getTag();
                saveAndApplyBackgroundColor(selectedColor);
                dialog.dismiss();
                Toast.makeText(NotesListActivity.this, R.string.successful_save_theme_color, Toast.LENGTH_SHORT).show();
            });
        }

        // 7. Set the listener for real-time updates for the SeekBar
        hueSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private int currentColor = getCurrentBackgroundColor();
            // Fixed Saturation and Value to ensure the colors are soft/pastel, not neon
            private final float SATURATION = 0.2f;
            private final float VALUE = 1.0f;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    float hue = (float) progress;
                    // Generate color using HSV
                    float[] hsv = {hue, SATURATION, VALUE};
                    currentColor = Color.HSVToColor(hsv);

                    // Update the preview in the dialog
                    colorPreview.setBackgroundColor(currentColor);

                    // Temporarily apply to activity background for real-time feedback
                    if (drawerLayout != null) {
                        drawerLayout.setBackgroundColor(currentColor);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Not saving here, just tracking the start of the interaction
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // User finished selecting the color, save it permanently
                saveAndApplyBackgroundColor(currentColor);
                Toast.makeText(NotesListActivity.this, R.string.successful_save_theme_color, Toast.LENGTH_SHORT).show();
            }
        });

        // 8. Show the dialog
        dialog.show();
    }
    // --- Background Color Persistence Variables ---
    private static final String PREFS_NAME = "NotesPrefs";
    private static final String BACKGROUND_COLOR_KEY = "BackgroundColor";
    private static final int DEFAULT_BACKGROUND_COLOR = Color.BLACK;
    private void saveAndApplyBackgroundColor(int color) {
        // 1. Save color
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putInt(BACKGROUND_COLOR_KEY, color)
                .apply();

        // 2. Apply color
        if (drawerLayout != null) {
            drawerLayout.setBackgroundColor(color);
        }
    }
    /**
     * Loads the saved background color from SharedPreferences and applies it to the drawerLayout.
     */
    private void loadAndApplyBackgroundColor() {
        int color = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getInt(BACKGROUND_COLOR_KEY, DEFAULT_BACKGROUND_COLOR);
        if (drawerLayout != null) {
            drawerLayout.setBackgroundColor(color);
        }
    }
    private void showSortDialog() {
        final String[] options = {getApplicationContext().getString(R.string.sort_by_date), getApplicationContext().getString(R.string.sort_by_alpha)};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.sort_notes_by);
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // Sort by Date
                    sortNotesByDate();
                    break;
                case 1: // Sort Alphabetically
                    sortNotesAlphabetically();
                    break;
            }
        });
        builder.show();
    }
    /**
     * Sorts the notes in both lists (pinned and unpinned) by date in descending order.
     */
    private void sortNotesByDate() {
        // Sort the unpinned notes
        Collections.sort(notesList, (note1, note2) -> {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            try {
                Date date1 = dateFormat.parse(note1.getDate());
                Date date2 = dateFormat.parse(note2.getDate());
                // Sort in descending order (newest first)
                return date2.compareTo(date1);
            } catch (ParseException e) {
                e.printStackTrace();
                return 0;
            }
        });
        notesAdapter.notifyDataSetChanged();
/*
        // Sort the pinned notes
        Collections.sort(pinnedNotes, (note1, note2) -> {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            try {
                Date date1 = dateFormat.parse(note1.getDate());
                Date date2 = dateFormat.parse(note2.getDate());
                // Sort in descending order (newest first)
                return date2.compareTo(date1);
            } catch (ParseException e) {
                e.printStackTrace();
                return 0;
            }
        });
        notesAdapterPinned.notifyDataSetChanged();*/
        Toast.makeText(this, R.string.notes_sorted_date, Toast.LENGTH_SHORT).show();
    }

    /**
     * Sorts the notes in both lists (pinned and unpinned) alphabetically by title.
     */
    private void sortNotesAlphabetically() {
        // Sort the unpinned notes
        Collections.sort(notesList, (note1, note2) -> {
            String title1 = note1.getTitle() != null ? note1.getTitle() : "";
            String title2 = note2.getTitle() != null ? note2.getTitle() : "";
            return title1.compareToIgnoreCase(title2);
        });
        notesAdapter.notifyDataSetChanged();
        /*
        // Sort the pinned notes
        Collections.sort(pinnedNotes, (note1, note2) -> {
            String title1 = note1.getTitle() != null ? note1.getTitle() : "";
            String title2 = note2.getTitle() != null ? note2.getTitle() : "";
            return title1.compareToIgnoreCase(title2);
        });
        notesAdapterPinned.notifyDataSetChanged();*/
        Toast.makeText(this, R.string.notes_sorted_alphabetically, Toast.LENGTH_SHORT).show();
    }
    private void hideOptions(final Animation animation) {
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                optionsLayout.setVisibility(View.GONE);
                optionsLayout.clearAnimation();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        optionsLayout.startAnimation(animation);
        isOptionsVisible = false;
    }
    private boolean isTouchInsideView(View view, MotionEvent event) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];

        return !(event.getRawX() < x || event.getRawX() > x + view.getWidth() ||
                event.getRawY() < y || event.getRawY() > y + view.getHeight());
    }
    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(noteUpdateReceiver,
                new IntentFilter("com.noteaiapp.ACTION_NOTE_UPDATED"));
        loadNotesFromDatabase();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();  // closes all activities in the task
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (actionMode != null) {
            actionMode.finish();
        }
    }
    @Override
    public void onPause(){
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(noteUpdateReceiver);
    }
    private void showOptions() {
        optionsLayout.setVisibility(View.VISIBLE);
        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(250);
        optionsLayout.startAnimation(fadeIn);
        transparentOverlay.setVisibility(View.VISIBLE);
        isOptionsVisible = true;
    }
    private void hideOptions() {
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.fab_options_slide_down);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                optionsLayout.setVisibility(View.GONE);
                transparentOverlay.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        optionsLayout.startAnimation(animation);
        isOptionsVisible = false;
    }
    // Add this new method anywhere inside your NotesListActivity class
    private void syncNotesFromFirebase(FirestoreSyncCallback callback) {
        if (currentUser == null) {
            Log.w(TAG, "Cannot sync notes from cloud, user is not logged in.");
            return; // Don't proceed if there's no user
        }

        String userId = currentUser.getUid();
        Log.d(TAG, "Starting sync from Firestore for user: " + userId);

        // Optional: Show a loading indicator to the user
        // For example, if you have a SwipeRefreshLayout:
        // swipeRefreshLayout.setRefreshing(true);
        List<Note> notesListFromFirestore = new ArrayList<>();

        // This is the query to get all notes for the current user
        db.collection("users").document(userId).collection("notes")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("com.noteaiapp.keyboardai", "Successfully fetched " + task.getResult().size() + " notes from Firestore.");

                        // Perform the heavy database operations on a background thread
                        for (QueryDocumentSnapshot document : task.getResult()) {
                                // Convert each document from Firestore into a Note object
                            Note cloudNote = document.toObject(Note.class);
                            notesListFromFirestore.add(cloudNote);
                            }
                        callback.onSyncComplete(notesListFromFirestore);

                    } else {
                        Log.w("com.noteaiapp.keyboardai", "Error getting documents from Firestore: ", task.getException());
                        callback.onSyncFailed(task.getException());
                    }
                    // Optional: Hide your loading indicator here
                    // swipeRefreshLayout.setRefreshing(false);
                });
    }

    // DELETE your old loadNotesFromDatabase method and REPLACE it with this new version.
    private void loadNotesFromDatabase() {
        syncNotesFromFirebase(new FirestoreSyncCallback() {
            @Override
            public void onSyncComplete(List<Note> syncedNotes) {
                Log.d(TAG, "Sync complete. Processing " + syncedNotes.size() + " notes.");
                Executors.newSingleThreadExecutor().execute(() -> {

                    // Prepare to filter notes for UI display
                    List<Note> filteredNotesForUi = new ArrayList<>();

                    // Filter the list of notes we just received
                    for (Note note : syncedNotes) {
                        if (note.getFontFamily() == null || note.getFontFamily().isEmpty() ||
                                (!note.getFontFamily().equalsIgnoreCase("archived") && !globalfolderlist.contains(note.getFontFamily()))) {
                            filteredNotesForUi.add(note);
                        }
                    }

                    // Update the UI on the main thread with the final, filtered list.
                    runOnUiThread(() -> {
                        allNotes.clear();
                        allNotes.addAll(filteredNotesForUi);
                        allNotesFromDb.clear();
                        allNotesFromDb.addAll(filteredNotesForUi);
                        notesList.clear();
                        notesList.addAll(allNotes);
                        notesAdapter.notifyDataSetChanged();
                        updatePinnedSectionVisibility();
                        Log.d(TAG, "UI has been refreshed with synced notes.");
                    });
                });
            }

            @Override
            public void onSyncFailed(Exception e) {
                // Handle the failure case
                runOnUiThread(() -> {
                    Toast.makeText(NotesListActivity.this, "Failed to sync notes.", Toast.LENGTH_SHORT).show();
                    // Hide the loading indicator.
                    // swipeRefreshLayout.setRefreshing(false);
                    loadNotesFromLocalDatabase();

                });
            }
        });
    }
    private void loadNotesFromLocalDatabase() {

        new Thread(() -> {
            if (currentUser != null) {
                List<Note> allNotesFromDb1 = noteRepository.getAllNotes();
                allNotesFromDb.clear();
                // get all folder names from sharepreference
                SharedPreferences prefs = getSharedPreferences("notes_app_folders", MODE_PRIVATE);
                // get folder names from prefs
                Set<String> folders = prefs.getStringSet("folder_set", new HashSet<>());
                for (Note note : allNotesFromDb1) {
                    if (note.getFontFamily() == null || note.getFontFamily().length() == 0 || (!note.getFontFamily().equalsIgnoreCase("archived") && !folders.contains(note.getFontFamily()))) {
                        //check if the note.getFontFamily is not the name of any folder
                        allNotesFromDb.add(note);
                    }
                }
                runOnUiThread(() -> {
                    allNotes.clear();
                    allNotes.addAll(allNotesFromDb);
                    notesList.clear();
                    notesList.addAll(allNotes);
                    notesAdapter.notifyDataSetChanged();
                    // notesAdapterPinned.notifyDataSetChanged();
                    updatePinnedSectionVisibility();
                });
        }
        }).start();
    }

    private void filterNotes(String query) {
        List<Note> masterUnpinned = (allNotesFromDb != null) ? allNotesFromDb : new ArrayList<>();
      //  List<Note> masterPinned   = (allPinnedNotesFromDb != null) ? allPinnedNotesFromDb : new ArrayList<>();

        // Create new lists to hold the filtered results.
        List<Note> filteredNotesList = new ArrayList<>();

        if (query == null || query.isEmpty()) {
            // If the query is empty, add all notes back from the master lists.
            filteredNotesList.addAll(masterUnpinned);
        } else {
            String lowercaseQuery = query.toLowerCase();

            // Filter un-pinned notes from the master list.
            for (Note note : allNotesFromDb) {
                boolean titleMatches = note.getTitle() != null && note.getTitle().toLowerCase().contains(lowercaseQuery);
                boolean contentMatches = note.getContent() != null && note.getContent().toLowerCase().contains(lowercaseQuery);

                if (titleMatches || contentMatches) {
                    filteredNotesList.add(note);
                }
            }

            /* Filter pinned notes from the master list.
            for (Note note : allPinnedNotesFromDb) {
                boolean titleMatches = note.getTitle() != null && note.getTitle().toLowerCase().contains(lowercaseQuery);
                boolean contentMatches = note.getContent() != null && note.getContent().toLowerCase().contains(lowercaseQuery);

                if (titleMatches || contentMatches) {
                    filteredPinnedNotesList.add(note);
                }
            }*/
        }

        // Update the adapters with the filtered lists.
        notesList.clear();
        notesList.addAll(filteredNotesList);
        notesAdapter.notifyDataSetChanged();

       // pinnedNotes.clear();
       // pinnedNotes.addAll(filteredPinnedNotesList);
       // notesAdapterPinned.notifyDataSetChanged();

        updatePinnedSectionVisibility();
    }

    public void updatePinnedSectionVisibility(){
        if(allNotes.size() == 0){
            initialtext.setVisibility(View.VISIBLE);
        }
        else{
            initialtext.setVisibility(View.GONE);
        }
    }
    private ActionMode.Callback actionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.menu_contextual_action_bar, menu);
            searchView.setVisibility(View.GONE);
            toolbar.setVisibility(View.GONE);
            drawerLayout.setBackgroundColor(Color.argb(43,135,73,251));
            fabAddNote.setVisibility(View.GONE);
            optionsLayout.setVisibility(View.GONE);

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (selectedNote == null) {
                mode.finish();
                return true;
            }

            int id = item.getItemId();

            if (id == R.id.action_share) {

                // 1. Check if it's an image/drawing note
                if (selectedNote.getImagePath() != null && !selectedNote.getImagePath().isEmpty()) {
                    try {
                        File imageFile = new File(selectedNote.getImagePath());

                        if (!imageFile.exists()) {
                            mode.finish();
                            return true;
                        }

                        // ---- Convert transparent image to white background ----
                        Bitmap originalBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                        if (originalBitmap == null) {
                            mode.finish();
                            return true;
                        }

                        Bitmap newBitmap = Bitmap.createBitmap(
                                originalBitmap.getWidth(),
                                originalBitmap.getHeight(),
                                Bitmap.Config.ARGB_8888
                        );

                        Canvas canvas = new Canvas(newBitmap);
                        canvas.drawColor(Color.WHITE); // white background
                        canvas.drawBitmap(originalBitmap, 0, 0, null);

                        File rootDir = getApplicationContext().getFilesDir();
                        // Save the processed bitmap into cache directory
                        File cachePath = new File(rootDir, "drawing_notes");
                        if (!cachePath.exists()) cachePath.mkdirs();
                        File newImageFile = new File(cachePath, "shared_image.png");

                        FileOutputStream fos = new FileOutputStream(newImageFile);
                        newBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        fos.close();


                        //File directory = context.getDir("images", Context.MODE_PRIVATE);

                        // Get content URI using FileProvider
                        Uri contentUri = FileProvider.getUriForFile(
                                NotesListActivity.this,
                                getApplicationContext().getPackageName() + ".fileprovider",
                                newImageFile
                        );

                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("image/*");
                        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);

                        // Add optional text
                        String title = selectedNote.getTitle() != null ? selectedNote.getTitle().split(";")[0] : "";
                        String content = (selectedNote.getContent() != null)? selectedNote.getContent():"";
                        String cleanContent = "";
                        if(content.length() > 0) {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                                cleanContent = Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY).toString();
                            } else {
                                cleanContent = Html.fromHtml(content).toString();
                            }
                        }
                        String shareText = "Title: " + title + "\n\n" +
                                "Description: " + (cleanContent);
                        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        startActivity(Intent.createChooser(shareIntent, "Share image note via"));

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else {
                    // 2. This is a text note - share as text
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");

                    String title = selectedNote.getTitle() != null ? selectedNote.getTitle().split(";")[0] : "";
                    String content = selectedNote.getContent() != null ? selectedNote.getContent() : "";
                    String cleanContent;

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        cleanContent = Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY).toString();
                    } else {
                        cleanContent = Html.fromHtml(content).toString();
                    }

// 3. Construct the final share text using the clean content
                    String shareText = title + "\n\n" + cleanContent;

                    shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

                    startActivity(Intent.createChooser(shareIntent, "Share text note via"));
                }

                mode.finish();
                return true;
            }  /*else if (id == R.id.action_pin) {
                final List<Note> selectedNotesToPin = notesAdapter.getSelectedNotes();
                //final List<Note> selectedPinnedNotesToUnpin = notesAdapterPinned.getSelectedNotes();

                // Determine if we are pinning or unpinning.
                boolean isPinning = !selectedNotesToPin.isEmpty();

                Executors.newSingleThreadExecutor().execute(() -> {
                    if (isPinning) {
                        noteRepository.updateNotePinStatusBulk(selectedNotesToPin, true);
                    } else {
                        noteRepository.updateNotePinStatusBulk(selectedPinnedNotesToUnpin, false);
                    }

                    runOnUiThread(() -> {
                        loadNotesFromDatabase();
                        Toast.makeText(NotesListActivity.this, isPinning ? "Notes pinned" : "Notes unpinned", Toast.LENGTH_SHORT).show();
                        mode.finish();
                    });
                });
                return true;
            }*/
            else if (id == R.id.action_delete_note) {
                final List<Note> selectedNotes = notesAdapter.getSelectedNotes();
               // final List<Note> selectedPinnedNotes = notesAdapterPinned.getSelectedNotes();

                if (selectedNotes.isEmpty()) {
                    mode.finish();
                    return true;
                }

                Executors.newSingleThreadExecutor().execute(() -> {
                    // Delete notes from the main list
                    for (Note note : selectedNotes) {
                        notesRepositoryTrash.addNote(note);
                        noteRepository.deleteNote(note.getId());
                        // delete note from firestore as well
                        db.collection("users").document(currentUser.getUid()).collection("notes").document(note.getUserFirebaseId()).delete();
                    }
                    /* Delete notes from the pinned list
                    for (Note note : selectedPinnedNotes) {
                        notesRepositoryTrash.addNote(note);
                        noteRepository.deleteNote(note.getId());
                    }*/

                    runOnUiThread(() -> {
                        // Reload data to reflect changes
                        loadNotesFromDatabase();
                        Toast.makeText(NotesListActivity.this, "Notes Deleted", Toast.LENGTH_SHORT).show();
                        mode.finish();
                    });
                });
                return true;
            }
            else if(id == R.id.action_archive){
                final List<Note> selectedNotes = notesAdapter.getSelectedNotes();
                // final List<Note> selectedPinnedNotes = notesAdapterPinned.getSelectedNotes();
                if (selectedNotes.isEmpty()) {
                    mode.finish();
                    return true;
                }
                Executors.newSingleThreadExecutor().execute(() -> {
                    // Delete notes from the main list
                    for (Note note : selectedNotes) {
                        note.setFontFamily("archived");
                        noteRepository.updateNoteByCloudId(note);
                        // update note in firestore as well
                        db.collection("users").document(currentUser.getUid()).collection("notes").document(note.getUserFirebaseId()).set(note);
                    }
                    runOnUiThread(() -> {
                        loadNotesFromDatabase();
                        Toast.makeText(NotesListActivity.this, "Notes Archived", Toast.LENGTH_SHORT).show();
                        mode.finish();
                    });
                });
                return true;
            }

            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            selectedNote = null;
            searchView.setVisibility(View.VISIBLE);
            fabAddNote.setVisibility(View.VISIBLE);
            toolbar.setVisibility(View.VISIBLE);
            drawerLayout.setBackgroundColor(Color.argb(71,0,0,0));

            notesAdapter.clearSelections();
           // notesAdapterPinned.clearSelections();
        }
    };

    private File createImageFile() throws IOException {
        // 1. Create a unique file name with a timestamp
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        // 2. Get the directory for storing the image.
        //    Use getExternalFilesDir() for persistent media, or getExternalCacheDir() for temporary files.
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        // 3. Ensure the directory exists.
        //    This is the crucial step that prevents the "No such file or directory" error.
        if (storageDir != null && !storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                Log.d("NotesListActivity", "Failed to create directory");
                return null; // Return null if directory creation fails
            }
        }

        // 4. Create the temporary image file in the directory
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        // mCurrentPhotoPath = image.getAbsolutePath(); // If you need to save the path
        return image;
    }


    /**
     * Copies an image from a source URI (camera or gallery) to our app's private, permanent storage.
     * @param sourceUri The URI of the image to copy.
     * @return The absolute path of the newly saved image, or null on failure.
     */
    private String saveImageToAppStorage(Uri sourceUri) {
        try {
            // Create a destination file in the app's private files directory
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File destinationFile = new File(getFilesDir(), "note_image_" + timeStamp + ".jpg");

            // Open an input stream from the source URI
            InputStream inputStream = getContentResolver().openInputStream(sourceUri);
            // Open an output stream to the destination file
            FileOutputStream outputStream = new FileOutputStream(destinationFile);

            // Copy the bytes
            byte[] buf = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }

            // Close the streams
            outputStream.close();
            inputStream.close();

            // Return the absolute path of our new file
            return destinationFile.getAbsolutePath();

        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Starts the ImageNoteActivity with the path of the saved image.
     * @param imagePath The path to the image in our app's storage.
     */
    private void launchImageNoteActivity(String imagePath) {
        Intent intent = new Intent(NotesListActivity.this, ImageNoteActivity.class);
        // We pass the image path so the activity knows which image to load.
        // The note doesn't exist yet, so we don't pass a note_id.
        intent.putExtra("image_path", imagePath);
        startActivity(intent);
    }
}
