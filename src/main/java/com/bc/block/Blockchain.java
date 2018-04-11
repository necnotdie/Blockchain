package com.bc.block;


import com.bc.util.Encrypt;
import com.bc.util.httputil.RequestUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Created by Administrator on 2018/3/6.
 */
public class Blockchain {
    private List<BlockBean> blockBeans;
    private List<Transaction> transactions;
    private Set<String> nodes;

    public List<BlockBean> getBlockBeans() {
        return blockBeans;
    }

    public Set<String> getNodes() {
        return nodes;
    }

    public Blockchain() {
        this.blockBeans = new ArrayList<BlockBean>();
        this.transactions = new ArrayList<Transaction>();
        this.nodes = new HashSet<String>();
        new_block("1", 100);
    }

    public synchronized BlockBean new_block(String previous_hash, int proof) {
        BlockBean blockBean = new BlockBean();
        blockBean.setIndex(blockBeans.size() + 1);
        blockBean.setProof(proof);
        blockBean.setTimestamp(new Date().getTime());
        blockBean.setTransactions(transactions);
        blockBean.setPrevious_hash(previous_hash == null ? hash(last_block()) : previous_hash);
        blockBeans.add(blockBean);
        transactions = new ArrayList<Transaction>();
        return blockBean;
    }

    public synchronized int new_transaction(String sender, String recipient, BigDecimal amount) {
        Transaction transaction = new Transaction();
        transaction.setSender(sender);
        transaction.setRecipient(recipient);
        transaction.setAmount(amount);
        transactions.add(transaction);
        return this.last_block().getIndex() + 1;
    }

    public void register_node(String address) throws MalformedURLException {
        URL url = new URL(address);
        nodes.add(url.getHost() + ":" + (url.getPort() == -1 ? url.getDefaultPort() : url.getPort()));
    }

    public boolean valid_chain(List<BlockBean> blockBeans) {
        BlockBean last_block = blockBeans.get(0);
        int current_index = 1;
        while (current_index < blockBeans.size()) {
            BlockBean block = blockBeans.get(current_index);
            if (!block.getPrevious_hash().equals(hash(last_block))) {
                return false;
            }
            if (!valid_proof(last_block.getProof(), block.getProof())) {
                return false;
            }
            last_block = block;
            current_index++;
        }
        return true;
    }

    public boolean ressolve_conflicts() {
        Set<String> nighbours = this.nodes;
        List<BlockBean> new_blockBeans = null;
        int max_length = this.blockBeans.size();
        for (String node : nighbours) {
            Object[] response = RequestUtil.HttpRequest("http://" + node + "/chain", "get", null);
            if (response != null) {
                String result = (String) response[0];
                Map<String, List<String>> responsehead = (Map<String, List<String>>) response[1];
                if (responsehead.get(null).get(0).contains("200")) {
                    JSONObject root = JSONObject.fromObject(result);
                    int length = Integer.parseInt(root.get("length").toString());
                    JSONArray blockArrays = root.getJSONArray("chain");
                    List<BlockBean> blockBeans = new ArrayList<BlockBean>(JSONArray.toCollection(blockArrays, BlockBean.class));
                    if (length > max_length && valid_chain(blockBeans)) {
                        max_length = length;
                        new_blockBeans = blockBeans;
                    }
                }
            }
        }
        if (new_blockBeans != null) {
            this.blockBeans = new_blockBeans;
            return true;
        }
        return false;
    }

    public boolean valid_proof(int last_proof, int proof) {
        StringBuffer sb = new StringBuffer(last_proof);
        sb.append(proof);
        return Encrypt.SHA256(sb.toString()).startsWith("0000");
    }

    public String hash(BlockBean blockBean) {
        JSONObject jsonObject = JSONObject.fromObject(blockBean);
        return Encrypt.SHA256(jsonObject.toString());
    }

    public int proof_of_work(int last_proof) {
        int proof = 0;
        while (!valid_proof(last_proof, proof)) {
            proof++;
        }
        return proof;
    }

    public BlockBean last_block() {
        return blockBeans.get(blockBeans.size() - 1);
    }
}
