package chat.cliente;

import chat.objeto.Mensagem;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class ChatSwingClient extends JFrame {

    private JTextField textIP;
    private JTextField textPorta;
    private JTextField textNome;
    private JTextField textMensagem;
    private JTextPane textPaneHistorico;
    private StyledDocument doc;
    private JButton buttonConectar;
    private JButton buttonSair;
    private JButton buttonEnviar;
    private JButton buttonLimparSelecao;
    private JList<String> listViewUsuarios;
    private DefaultListModel<String> listModelUsuarios;

    private ThreadCliente threadCliente;

    // Mapa para associar cor fixa para cada usuário
    private Map<String, Color> mapaCoresUsuarios = new HashMap<>();
    private Color[] coresDisponiveis = { Color.BLUE, Color.MAGENTA, Color.ORANGE, Color.GREEN, Color.RED, Color.CYAN, Color.PINK, Color.DARK_GRAY };
    private int corIndex = 0;

    public ChatSwingClient() {
        setTitle("Chat Cliente - Swing");
        setSize(600, 450);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Painel superior - conexão
        JPanel panelConexao = new JPanel(new GridBagLayout());
        panelConexao.setBorder(BorderFactory.createTitledBorder("Conexão"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        textIP = new JTextField("127.0.0.1", 10);
        textPorta = new JTextField("54321", 5);
        textNome = new JTextField(10);

        buttonConectar = new JButton("Conectar");
        buttonSair = new JButton("Sair");

        gbc.gridx = 0; gbc.gridy = 0; panelConexao.add(new JLabel("IP Servidor"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; panelConexao.add(textIP, gbc);
        gbc.gridx = 2; gbc.gridy = 0; panelConexao.add(new JLabel("Porta"), gbc);
        gbc.gridx = 3; gbc.gridy = 0; panelConexao.add(textPorta, gbc);

        gbc.gridx = 0; gbc.gridy = 1; panelConexao.add(new JLabel("Nome"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; panelConexao.add(textNome, gbc);
        gbc.gridx = 2; gbc.gridy = 1; panelConexao.add(buttonConectar, gbc);
        gbc.gridx = 3; gbc.gridy = 1; panelConexao.add(buttonSair, gbc);

        add(panelConexao, BorderLayout.NORTH);

        // Painel central - histórico + mensagem
        JPanel panelCentro = new JPanel(new BorderLayout(5, 5));

        textPaneHistorico = new JTextPane();
        textPaneHistorico.setEditable(false);
        doc = textPaneHistorico.getStyledDocument();

        panelCentro.add(new JLabel("Histórico"), BorderLayout.NORTH);
        panelCentro.add(new JScrollPane(textPaneHistorico), BorderLayout.CENTER);

        // Painel inferior - mensagem
        JPanel panelMensagem = new JPanel(new BorderLayout(5, 5));
        textMensagem = new JTextField();
        buttonEnviar = new JButton("Enviar");

        panelMensagem.add(new JLabel("Mensagem"), BorderLayout.NORTH);
        panelMensagem.add(textMensagem, BorderLayout.CENTER);
        panelMensagem.add(buttonEnviar, BorderLayout.EAST);

        panelCentro.add(panelMensagem, BorderLayout.SOUTH);

        add(panelCentro, BorderLayout.CENTER);

        // Painel lateral - usuários
        JPanel panelUsuarios = new JPanel(new BorderLayout());
        panelUsuarios.setBorder(BorderFactory.createTitledBorder("Usuários"));

        listModelUsuarios = new DefaultListModel<>();
        listViewUsuarios = new JList<>(listModelUsuarios);
        buttonLimparSelecao = new JButton("Limpar seleção");
        buttonLimparSelecao.addActionListener(e -> listViewUsuarios.clearSelection());

        panelUsuarios.add(new JScrollPane(listViewUsuarios), BorderLayout.CENTER);
        panelUsuarios.add(buttonLimparSelecao, BorderLayout.SOUTH);

        add(panelUsuarios, BorderLayout.EAST);

        // Botões
        buttonConectar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                inicializarThreadCliente();
            }
        });

        buttonSair.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                desconectar();
            }
        });

        buttonEnviar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enviarMensagem();
            }
        });

        // Enter no campo mensagem também envia
        textMensagem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enviarMensagem();
            }
        });

        setVisible(true);
    }

    private void inicializarThreadCliente() {
        String ip = textIP.getText();
        int porta = Integer.parseInt(textPorta.getText());
        String nomeUsuario = textNome.getText();

        threadCliente = new ThreadCliente(mensagem -> {
            SwingUtilities.invokeLater(() -> {
                if (mensagem.getAction() == Mensagem.Action.USERS_ONLINE) {
                    atualizarListaUsuarios(mensagem.getUsuariosOnline());
                } else {
                    Color cor = getCorParaUsuario(mensagem.getRemetente());
                    String textoFormatado = mensagem.getRemetente() + ": " + mensagem.getTexto();
                    appendMensagemColorida(textoFormatado, cor);
                }
            });
        }, nomeUsuario, ip, porta);

        threadCliente.start();
    }

    private void desconectar() {
        if (threadCliente != null) {
            threadCliente.desconectar();
            threadCliente = null;
        }
    }

    private void enviarMensagem() {
        if (threadCliente == null) {
            JOptionPane.showMessageDialog(this, "Você não está conectado.");
            return;
        }

        String texto = textMensagem.getText();
        if (texto.trim().isEmpty()) {
            return;
        }

        String destinatario = listViewUsuarios.getSelectedValue();

        if (destinatario == null) {
            threadCliente.enviarMensagem(texto, null, Mensagem.Action.SEND);
        } else {
            threadCliente.enviarMensagem(texto, destinatario, Mensagem.Action.SEND_ONE);
        }

        textMensagem.setText("");
    }

    private void atualizarListaUsuarios(List<String> usuarios) {
        listModelUsuarios.clear();
        for (String usuario : usuarios) {
            if (!usuario.equals(textNome.getText())) { // não mostrar a si mesmo
                listModelUsuarios.addElement(usuario);
            }
        }
    }

    // Método que associa uma cor fixa para cada usuário
    private Color getCorParaUsuario(String usuario) {
        if (!mapaCoresUsuarios.containsKey(usuario)) {
            mapaCoresUsuarios.put(usuario, coresDisponiveis[corIndex % coresDisponiveis.length]);
            corIndex++;
        }
        return mapaCoresUsuarios.get(usuario);
    }

    // Método que adiciona mensagem colorida no JTextPane
    private void appendMensagemColorida(String texto, Color cor) {
        Style style = textPaneHistorico.addStyle("Estilo", null);
        StyleConstants.setForeground(style, cor);
        try {
            doc.insertString(doc.getLength(), texto + "\n", style);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatSwingClient());
    }
    
}
