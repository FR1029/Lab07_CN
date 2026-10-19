import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class VN {
    static class LSTuple {
        char id;
        InetAddress ip;
        int port;
        int cost;
    }
    static int[][] graph = new int[26][26];
    static int n = 0;
    static int MAX_NODES = 26;
    static int[] new_seq = new int[26];

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Usage: java VN <ON-IP> <VN-IP> <VN-UDP-Port>");
            System.exit(1);
        }

        for (int i = 0; i < MAX_NODES; i++) {
            for(int j = 0; j < MAX_NODES; j++) graph[i][j] = (i == j) ? 0 : -1;
            new_seq[i] = -1;
        }

        String on_ip = args[0];
        String vn_ip = args[1];
        int own_udp_port = Integer.parseInt(args[2]);
        char my_id = '?';

        DatagramSocket udp_socket = new DatagramSocket(own_udp_port);
        udp_socket.setSoTimeout(10000);

        System.out.println("Connnecting to Oracle Node at " + on_ip + ":5000...");
        Socket tcp_socket = new Socket(on_ip, 5000);
        DataOutputStream out = new DataOutputStream(tcp_socket.getOutputStream());
        DataInputStream in = new DataInputStream(tcp_socket.getInputStream());

        InetAddress vn_address = InetAddress.getByName(vn_ip);
        byte[] my_ip_bytes = vn_address.getAddress();
        
        if (my_ip_bytes.length != 4) {
            throw new IOException("VN-IP must be an IPv4 address");
        }
        ByteBuffer connect_msg = ByteBuffer.allocate(6);
        connect_msg.put(my_ip_bytes);
        connect_msg.putShort((short) own_udp_port);

        out.write(connect_msg.array());
        out.flush();

        System.out.println("Sent CONNECt: " + vn_address.getHostAddress() + ":" + own_udp_port);

        int link_count = 0;
        List<LSTuple> links = new ArrayList<>();
        int seq_num = 0;
        byte[] data_received = new byte[2048];

        System.out.println("Entering Link_State Event Loop: ");
        while(true) {
            try {
                if(in.available() > 0) {
                    link_count = in.readInt();
                    links.clear();
                    System.out.println("\n Received Updated Link State from ON");

                    for(int i = 0; i < link_count; i++) {
                        LSTuple tuple = new LSTuple();
                        tuple.id = (char) in.readByte();

                        byte[] ip_bytes = new byte[4];
                        in.readFully(ip_bytes);
                        tuple.ip = InetAddress.getByAddress(ip_bytes);

                        tuple.port = in.readShort() & 0xFFF;
                        tuple.cost = in.readInt();
                        links.add(tuple);

                        if(tuple.cost == 0) my_id = tuple.id;
                        max_node(tuple.id);
                        System.out.println(tuple.id + "=" + tuple.cost);
                    }
                    System.out.println("-----------------------------------------");

                    int my_idx = my_id - 'A';
                    for (int i = 0; i < 26; i++) graph[my_idx][i] = (i == my_idx) ? 0 : -1;
                    for (LSTuple tuple : links) {
                        graph[my_idx][tuple.id - 'A'] = tuple.cost;
                        graph[tuple.id - 'A'][my_idx] = tuple.cost;
                    }
                    seq_num++;
                    broadcast_lsp(my_id, seq_num, links, udp_socket);
                }
                DatagramPacket packet_received = new DatagramPacket(data_received, data_received.length);
                udp_socket.receive(packet_received);
                ByteBuffer buffer = ByteBuffer.wrap(packet_received.getData(), 0, packet_received.getLength());
                char origin = (char) buffer.get();
                int seq = buffer.getInt();
                int from_idx = origin - 'A';
                if(seq > new_seq[from_idx] && origin!= my_id){
                    new_seq[from_idx] = seq;
                    System.out.println("Received & Flooded LSP from " + origin + " Seq: " + seq + ")");

                    int num = buffer.getInt();
                    for(int i = 0; i < 26; i++) graph[from_idx][i] = (i == from_idx) ? 0 : -1;

                    for(int i = 0; i < num; i++){
                        char u_id = (char) buffer.get();
                        buffer.position(buffer.position() + 6);
                        int u_cost = buffer.getInt();
                        graph[from_idx][u_id - 'A'] = u_cost;
                        graph[u_id - 'A'][from_idx] = u_cost;
                        max_node(u_id);
                    }
                    for(LSTuple t : links) {
                        if(t.cost > 0){
                            DatagramPacket lsp = new DatagramPacket(packet_received.getData(), packet_received.getLength(), t.ip, t.port);
                            udp_socket.send(lsp);
                        }
                    }
                }
            } catch (SocketTimeoutException e) {
                if (links.isEmpty()) continue;
                seq_num++;
                broadcast_lsp(my_id, seq_num, links, udp_socket);
                print_graph();
            }
        }
    }

    private static void print_graph() {
        System.out.println("\n --- Periodically Printing Full Graph ---");
        for(int i = 0; i < n - 1; i++) {
            StringBuilder row = new StringBuilder();
            for (int t = 0; t < i; t++) row.append("\t");
            for(int j = i + 1; j < n; j++) row.append(graph[i][j]).append("\t");
            System.out.println(row.toString());
        }
        System.out.println("-----------------------------------------\n");
    }

    private static void max_node (char id) {
        int idx = id - 'A' + 1;
        if (idx > n) n = idx;
    }

    private static void broadcast_lsp(char my_id, int seq_num, List<LSTuple> links, DatagramSocket udp_socket) throws IOException {
        int packet_size = 1 + 4 + 4 + (links.size() * 11);
        ByteBuffer buffer = ByteBuffer.allocate(packet_size);
        buffer.put((byte) my_id);
        buffer.putInt(seq_num);
        buffer.putInt(links.size());

        for(LSTuple t : links) {
            buffer.put((byte) t.id);
            buffer.put(t.ip.getAddress());
            buffer.putShort((short) t.port);
            buffer.putInt(t.cost);
        }

        byte[] lsp_data = buffer.array();

        for (LSTuple t : links) {
            if (t.cost > 0) {
                DatagramPacket send_packet = new DatagramPacket(lsp_data, lsp_data.length, t.ip, t.port);
                udp_socket.send(send_packet);
            }
        }
        System.out.println("Broadcasted periodic LSP (Seq: " + seq_num + ") to neighbors.");
     }
}