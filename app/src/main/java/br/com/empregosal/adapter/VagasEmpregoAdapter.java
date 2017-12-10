package br.com.empregosal.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import br.com.empregosal.R;
import br.com.empregosal.config.ConfiguracaoFirebase;
import br.com.empregosal.helper.Preferencias;
import br.com.empregosal.model.Candidatura;
import br.com.empregosal.model.Empresa;
import br.com.empregosal.model.Usuario;
import br.com.empregosal.model.Vaga;

public class VagasEmpregoAdapter extends ArrayAdapter<Vaga> {

    private ArrayList<Vaga> vagas;
    private Context context;
    private Vaga vaga;
    private DatabaseReference firebase;
    private Query pesquisa;
    private Query consultaUsuario;
    private Query consultaEmpress;
    private Empresa empresaPesquisda;
    private Usuario usuarioPesquisado;
    private Candidatura candidaturaP;

    public VagasEmpregoAdapter(Context c, ArrayList<Vaga> objects) {
        super(c, 0, objects);
        this.vagas = objects;
        this.context = c;
    }

    public int Qtd() {
        return vagas.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = null;
        final int posicao = position;

        // Verifica se a lista está vazia
        if (vagas != null) {

            LayoutInflater inflater = (LayoutInflater) context.getSystemService(context.LAYOUT_INFLATER_SERVICE);

            view = inflater.inflate(R.layout.lista_vagas_usuario, parent, false);

            // recupera elemento para exibição
            TextView vagaCargo = view.findViewById(R.id.tv_vaga_emprego_cargo);
            TextView vagaArea = view.findViewById(R.id.tv_area_vaga_emprego);
            TextView vagaLocalizacao = view.findViewById(R.id.tv_vaga_emprego_localizacao);
            Button botaoCandidatar = view.findViewById(R.id.bt_candidatar_se);

            Preferencias preferencias = new Preferencias(getContext());
            final String idUsuarioLogado = preferencias.getIdentificador();

            vaga = vagas.get(position);
            vagaCargo.setText(vaga.getCargo());
            vagaArea.setText(vaga.getAreaProfissional());
            vagaLocalizacao.setText(vaga.getLocalizacao());

            consultaUsuario = ConfiguracaoFirebase.getFirebase().child("usuarios")
                    .orderByChild("idUsuario")
                    .equalTo(idUsuarioLogado);

            consultaUsuario.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    for (DataSnapshot dados : dataSnapshot.getChildren()) {
                        Usuario usuario = dados.getValue(Usuario.class);
                        usuarioPesquisado = usuario;
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(getContext(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

            consultaEmpress = ConfiguracaoFirebase.getFirebase().child("empresas")
                    .orderByChild("idEmpresa")
                    .equalTo(vaga.getIdEmpresa());

            consultaEmpress.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    for (DataSnapshot dados : dataSnapshot.getChildren()) {
                        Empresa empresa = dados.getValue(Empresa.class);
                        empresaPesquisda = empresa;
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(getContext(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

            botaoCandidatar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Função de vincular vaga ao usuário logado
                    candidatarVaga(posicao, idUsuarioLogado, empresaPesquisda, usuarioPesquisado, vaga);
                }
            });
        }
        return view;
    }

    private void candidatarVaga(int posicao, String idUsuarioLogado, Empresa empresaPesquisda, Usuario usuarioPesquisado, Vaga vaga) {
        vaga = vagas.get(posicao);
        candidaturaP = null;

        //Validar se já existe cadastro na vaga

        String idEmpresa = vaga.getIdEmpresa();
        final Candidatura candidatura = new Candidatura();
        candidatura.setIdUsuario(idUsuarioLogado);
        candidatura.setIdEmpresa(idEmpresa);
        candidatura.setNomeUsuario(usuarioPesquisado.getNome());
        candidatura.setNomeEmpresa(empresaPesquisda.getNome());
        candidatura.setNomeVaga(vaga.getCargo());
        candidatura.setIdVaga(vaga.getIdVaga());

        pesquisa = ConfiguracaoFirebase.getFirebase().child("candidaturas")
                .orderByChild("idVaga")
                .equalTo(vaga.getIdVaga());

        pesquisa.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot dados : dataSnapshot.getChildren()) {
                    candidaturaP = dados.getValue(Candidatura.class);
                }

                if (candidaturaP == null){

                    try {

                        firebase = ConfiguracaoFirebase.getFirebase().child("candidaturas");
                        firebase.push()
                                .setValue(candidatura);

                        Toast.makeText(getContext(), "Sucesso ao candidatar na vaga", Toast.LENGTH_LONG).show();

                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Erro ao candicatar na vaga", Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                }else{
                    Toast.makeText(getContext(), "Já cadastrado na vaga " + candidaturaP.getNomeVaga(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getContext(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}