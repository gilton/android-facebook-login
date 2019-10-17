package br.com.gcetech.mytinder;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.firebase.client.core.view.View;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    int contador;

    boolean podeSwipe = true;

    String nome;
    String email;
    String id;
    String caminhoImagem;

    ImageView imagemCima;
    ImageView imagemBaixo;
    ImageView icone;
    ImageView imagemUsuario;
    TextView textoNomeUsuario;
    TextView textoNomeAmigoAtual;
    TextView textoNomeProximoAmigo;

    public static String FIREBASE_URL = "https://meu-tinder.firebaseio.com/";
    RelativeLayout meuLayout;


    AccessToken accessToken;

    ArrayList<String> listaLikes;
    ArrayList<String> listaDeslikes;
    ArrayList<String> listaAmigos;
    ArrayList<String> listaLikesAmigoAtual;
    ArrayList<String> listaLikesProximoAmigo;

    String idAmigoAtual;
    String idProximoAmigo;

    Firebase amigosRef;
    Firebase likeRef;
    Firebase dislikeRef;

    ValueEventListener likeValueEventListener;
    ValueEventListener dislikeValueEventListener;
    ValueEventListener amigosValueEventListener;

    int amigoAtual = 0;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Firebase.setAndroidContext(this);





        icone = (ImageView) findViewById(R.id.icone);
        imagemCima = (ImageView) findViewById(R.id.imagemCima);
        imagemBaixo = (ImageView) findViewById(R.id.imagemBaixo);

        textoNomeAmigoAtual = (TextView) findViewById(R.id.nomeAmigoAtual);
        textoNomeProximoAmigo = (TextView) findViewById(R.id.nomeProximoAmigo);
        textoNomeUsuario = (TextView) findViewById(R.id.nomeUsuario);

        imagemUsuario = (ImageView) findViewById(R.id.imagemPerfilUsuario);

        meuLayout = (RelativeLayout) findViewById(R.id.meuLayout);


        imagemBaixo.setAlpha(0.0f);
        icone.setAlpha(0.0f);
        imagemBaixo.setRotation(-30.0f);
        imagemBaixo.setScaleX(0.35f);
        imagemBaixo.setScaleY(0.35f);






        meuLayout.setOnTouchListener(new OnSwipeTouchListener(this) {
            public void onSwipeRight() {
                animaFotos(false);
            }
            public void onSwipeLeft() {
                animaFotos(true);
            }
        });

        amigosRef = new Firebase(FIREBASE_URL);
        listaLikes = new ArrayList<>();
        listaDeslikes = new ArrayList<>();
        listaAmigos = new ArrayList<>();
        listaLikesAmigoAtual = new ArrayList<>();
        listaLikesProximoAmigo = new ArrayList<>();

    }


    public void animaFotos(boolean estado){

        float direcao;
        float giro;

        if (estado){
            direcao = -1000.0f;
            giro = -30.0f;
            icone.setImageResource(R.drawable.iconerrado);
        }else{
            direcao = 1000.0f;
            giro = 30.0f;
            icone.setImageResource(R.drawable.iconcerto);

            if(listaLikesAmigoAtual.contains(id)){
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setTitle("Novo Match!");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                builder.show();
            }
        }


//        if(podeSwipe){
//            contador++;
//            podeSwipe =false;
//
//            if(contador == listaAmigos.size()){
//                contador = 0;
//            }


        if(podeSwipe){
            if(estado){
                listaDeslikes.add(idAmigoAtual);
                dislikeRef.setValue(listaDeslikes);
            } else {
                listaLikes.add(idAmigoAtual);
                likeRef.setValue(listaLikes);
            }

            podeSwipe = false;

            imagemCima.animate()
                    .translationXBy(direcao)
                    .rotationBy(giro)
                    .setDuration(500);


            imagemBaixo.animate()
                    .rotationBy(30.0f)
                    .alphaBy(1.0f)
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(500);

            icone.animate()
                    .alphaBy(1.0f)
                    .setDuration(250);


            new CountDownTimer(750, 1000) {
                public void onTick(long millisecondsUntilDone) {
                    Log.i("LogX Seconds left", String.valueOf(millisecondsUntilDone / 1000));
                }

                public void onFinish() {
                    Log.i("LogX Done!", "Coundown Timer Finished");
                    trocaTexturas();
                    podeSwipe=true;
                    icone.animate()
                            .alphaBy(-1.0f)
                            .setDuration(250);
                }
            }.start();
        }

    }





    public void trocaTexturas(){
        imagemCima.setImageDrawable(imagemBaixo.getDrawable());
        textoNomeAmigoAtual.setText(textoNomeProximoAmigo.getText());

        imagemCima.animate()
                .translationX(0)
                .rotation(0)
                .setDuration(0);

        imagemBaixo.animate()
                .rotation(-30)
                .alpha(0)
                .scaleX(0.35f)
                .scaleY(0.35f)
                .setDuration(0);

        if(idProximoAmigo==null){
            podeSwipe=false;
        } else {
            idAmigoAtual = idProximoAmigo;
        }

        listaLikesAmigoAtual = (ArrayList<String>) listaLikesProximoAmigo.clone();
        listaLikesProximoAmigo.clear();

        idProximoAmigo = selectUser(imagemBaixo, textoNomeProximoAmigo, listaLikesProximoAmigo);
    }




    @Override
    protected void onStart() {
        super.onStart();

        accessToken = AccessToken.getCurrentAccessToken();
        if(accessToken == null){
            finish();
        }

        GraphRequest request = GraphRequest.newMeRequest(accessToken, new GraphRequest.GraphJSONObjectCallback() {
            @Override
            public void onCompleted(JSONObject object, GraphResponse response) {
                try {
                    nome = object.getString("name");
                    email = object.getString("email");
                    id = object.getString("id");

                    textoNomeUsuario.setText(nome);

                    caminhoImagem = "https://graph.facebook.com/" + object.getString("id") + "/picture?type=large";

//                  Picasso.with(MainActivity.this).load(caminhoImagem).into(imagemUsuario);
                    Picasso.get().load(caminhoImagem).into(imagemUsuario);

                    Map<String, Object> infos = new HashMap<>();
                    infos.put("id", id);
                    infos.put("nome", nome);
                    infos.put("email", email);
                    infos.put("imgurl", caminhoImagem);

                    amigosRef.child(id).updateChildren(infos);
                    likeRef = new Firebase(FIREBASE_URL + "/" + id + "/like");
                    dislikeRef = new Firebase(FIREBASE_URL + "/" + id + "/dislike");

                    likeRef.addValueEventListener(likeValueEventListener);
                    dislikeRef.addValueEventListener(dislikeValueEventListener);
                    amigosRef.addListenerForSingleValueEvent(amigosValueEventListener);


                }catch(JSONException e) {e.printStackTrace();}
            }
        });

        Bundle parameters = new Bundle();
        parameters.putString("fields", "id,name,email");
        request.setParameters(parameters);
        request.executeAsync();


        likeValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                listaLikes.clear();
                for(DataSnapshot data:dataSnapshot.getChildren()){
                    listaLikes.add(data.getValue(String.class));
                }
                Log.d("meuLog", "like " + String.valueOf(listaLikes));
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        };

        dislikeValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                listaDeslikes.clear();
                for(DataSnapshot data:dataSnapshot.getChildren()){
                    listaDeslikes.add(data.getValue(String.class));
                }
                Log.d("meuLog", "dislike " + String.valueOf(listaDeslikes));
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        };

        amigosValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot data: dataSnapshot.getChildren()){
                    listaAmigos.add(data.child("id").getValue(String.class));
                }
                Log.d("meuLog", "Amigos " + String.valueOf(listaAmigos));

                idAmigoAtual = selectUser(imagemCima, textoNomeAmigoAtual, listaLikesAmigoAtual);
                idProximoAmigo = selectUser(imagemBaixo, textoNomeProximoAmigo, listaLikesProximoAmigo);


            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        };

    }

    public void btnSair(View view){
        LoginManager.getInstance().logOut();
        finish();
    }

    public String selectUser(ImageView imageView, TextView textView, ArrayList<String> pretListLike){
        for(int i=amigoAtual;i<listaAmigos.size();i++, amigoAtual++){
            if(!listaLikes.contains(listaAmigos.get(i)) && !listaDeslikes.contains(listaAmigos.get(i)) && !listaAmigos.get(i).equals(id)){
                setImagem(imageView, listaAmigos.get(i), textView, pretListLike);
                amigoAtual++;
                return listaAmigos.get(i);
            }

        }

        imagemBaixo.setImageResource(R.drawable.acabou);
        textoNomeProximoAmigo.setText("Você já viu todos!");

        return null;
    }

    public void setImagem(final ImageView imageView, String userId, final TextView textView, final ArrayList<String> pretListLike){
        amigosRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String imgurl = dataSnapshot.child("imgurl").getValue(String.class);
                String nome = dataSnapshot.child("nome").getValue(String.class);
                for(DataSnapshot data: dataSnapshot.child("like").getChildren()){
                    pretListLike.add(data.getValue(String.class));
                }

//              Picasso.with(MainActivity.this).load(imgurl).into(imageView);
                Picasso.get().load(imgurl).into(imageView);
                textView.setText(nome);

                Log.d("meuLog", "Set: " + nome + " " + imgurl);
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });

    }
}
