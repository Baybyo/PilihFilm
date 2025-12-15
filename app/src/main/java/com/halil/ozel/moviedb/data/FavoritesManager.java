package com.halil.ozel.moviedb.data;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser; // 🆕 Import
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.halil.ozel.moviedb.data.models.Results;

import java.util.ArrayList;
import java.util.List;

/**
 * Mengelola daftar film favorit menggunakan Firebase Firestore.
 * Data diisolasi per pengguna (User ID).
 * Semua operasi bersifat ASINKRON.
 */
public class FavoritesManager {

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    // Hapus: private final String userId; // Kita tidak lagi menyimpan ini secara statis

    /**
     * Konstruktor: Menginisialisasi koneksi Firebase.
     */
    public FavoritesManager() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        // Hapus inisialisasi userId di sini
    }

    /**
     * 🆕 Dapatkan UID pengguna saat ini.
     * @return UID pengguna, atau null jika belum login.
     */
    private String getUserId() {
        FirebaseUser user = auth.getCurrentUser();
        // Ini akan selalu mendapatkan status login terbaru.
        return user != null ? user.getUid() : null;
    }

    /**
     * Mengambil referensi koleksi Firestore untuk favorit pengguna saat ini.
     * @return CollectionReference ke /users/{userId}/favorites
     */
    private CollectionReference getFavoritesCollection() {
        String currentUserId = getUserId(); // 🆕 Ambil UID terbaru
        if (currentUserId == null) {
            throw new IllegalStateException("Pengguna harus login untuk mengakses favorit.");
        }
        return db.collection("users")
                .document(currentUserId) // Gunakan UID terbaru
                .collection("favorites");
    }

    /**
     * Menambahkan item (film) ke daftar favorit pengguna di Firestore.
     * Menggunakan ID film sebagai ID dokumen Firestore untuk mencegah duplikasi.
     * @param item Objek Results yang akan disimpan.
     * @return Task<Void> untuk memantau keberhasilan operasi.
     */
    public Task<Void> add(Results item) {
        String currentUserId = getUserId(); // 🆕 Cek UID terbaru
        if (currentUserId == null) return Tasks.forException(new IllegalStateException("User not logged in."));

        // Menggunakan ID film sebagai ID dokumen
        String movieId = String.valueOf(item.getId());

        // Menyimpan objek Results (film) sebagai dokumen baru
        return getFavoritesCollection().document(movieId).set(item);
    }

    /**
     * Menghapus item (film) dari daftar favorit.
     * @param id ID film yang akan dihapus.
     * @return Task<Void> untuk memantau keberhasilan operasi.
     */
    public Task<Void> remove(int id) {
        String currentUserId = getUserId(); // 🆕 Cek UID terbaru
        if (currentUserId == null) return Tasks.forException(new IllegalStateException("User not logged in."));
        String movieId = String.valueOf(id);

        // Menghapus dokumen berdasarkan ID film
        return getFavoritesCollection().document(movieId).delete();
    }

    /**
     * Memeriksa apakah suatu film merupakan favorit pengguna.
     * @param id ID film yang akan dicek.
     * @return Task<Boolean> yang akan menghasilkan true jika film ditemukan.
     */
    public Task<Boolean> isFavorite(int id) {
        String currentUserId = getUserId(); // 🆕 Cek UID terbaru
        if (currentUserId == null) return Tasks.forResult(false);
        String movieId = String.valueOf(id);

        return getFavoritesCollection().document(movieId).get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        // Mengembalikan true jika dokumen tersebut ada (exists)
                        return task.getResult().exists();
                    }
                    return false;
                });
    }

    /**
     * Memuat seluruh daftar film favorit pengguna.
     * @return Task<List<Results>> yang akan menghasilkan daftar film.
     */
    public Task<List<Results>> load() {
        String currentUserId = getUserId(); // 🆕 Cek UID terbaru
        if (currentUserId == null) return Tasks.forResult(new ArrayList<>());

        return getFavoritesCollection().get()
                .continueWith(task -> {
                    List<Results> favoritesList = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        QuerySnapshot snapshot = task.getResult();
                        // Mengkonversi setiap dokumen kembali ke objek Results
                        for (Results r : snapshot.toObjects(Results.class)) {
                            favoritesList.add(r);
                        }
                    }
                    return favoritesList;
                });
    }
}
//package com.halil.ozel.moviedb.data;
//
//import android.content.Context;
//import android.content.SharedPreferences;
//
//import com.google.gson.Gson;
//import com.google.gson.reflect.TypeToken;
//import com.halil.ozel.moviedb.data.models.Results;
//
//import java.lang.reflect.Type;
//import java.util.ArrayList;
//import java.util.List;
//
//public class FavoritesManager {
//
//    private static final String PREF_NAME = "favorites";
//    private static final String KEY_LIST = "list";
//
//    private static SharedPreferences prefs(Context context) {
//        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
//    }
//
//
//    private static final Gson gson = new Gson();
//    private static final Type listType = new TypeToken<List<Results>>() {}.getType();
//
//    public static List<Results> load(Context context) {
//        String json = prefs(context).getString(KEY_LIST, null);
//        if (json == null) return new ArrayList<>();
//        try {
//            return gson.fromJson(json, listType);
//        } catch (Exception e) {
//            return new ArrayList<>();
//        }
//    }
//
//    public static void save(Context context, List<Results> list) {
//        prefs(context).edit().putString(KEY_LIST, gson.toJson(list)).apply();
//    }
//
//    public static void add(Context context, Results item) {
//        List<Results> list = load(context);
//        for (Results r : list) {
//            if (r.getId().equals(item.getId())) return;
//        }
//        list.add(item);
//        save(context, list);
//    }
//
//    public static void remove(Context context, int id) {
//        List<Results> list = load(context);
//        List<Results> newList = new ArrayList<>();
//        for (Results r : list) {
//            if (!r.getId().equals(id)) {
//                newList.add(r);
//            }
//        }
//        save(context, newList);
//    }
//
//    public static boolean isFavorite(Context context, int id) {
//        List<Results> list = load(context);
//        for (Results r : list) {
//            if (r.getId().equals(id)) return true;
//        }
//        return false;
//    }
//}