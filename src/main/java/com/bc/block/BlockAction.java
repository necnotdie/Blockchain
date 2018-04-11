package com.bc.block;

import com.bc.annotation.RequestMapping;
import com.bc.annotation.RestController;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.util.*;

@RestController
public class BlockAction {
    private Blockchain blockchain = new Blockchain();

    @RequestMapping(value = "/test", produces = "")
    public String test(Transaction transaction) {
        System.out.println("=========" + transaction.getAmount());
        return "";
    }

    @RequestMapping(value = "/testlist", produces = "")
    public String testList(List<Transaction> transactions) {
        for (Transaction tran : transactions) {
            System.out.println("=========" + tran.getAmount());
        }
        return "";
    }

    @RequestMapping(value = "/testString", produces = "")
    public String testString(String aaa) {
        for (int i = 0; i < 5; i++) {

        }
        String bbb;
        System.out.println(aaa);
        return "";
    }

    @RequestMapping(value = "/testliststr", produces = "")
    public List<Transaction> testListStr(List<Transaction> transactions, String aaa) {
        for (Transaction tran : transactions) {
            System.out.println("=========" + tran.getAmount());
        }
//        for (int i = 0; i < transactions.size(); i++) {
//            System.out.println("=========" + transactions.get(i).getAmount());
//        }
        System.out.println(aaa);
        return transactions;
    }

    @RequestMapping(value = "/testblocklist", produces = "")
    public String testblocklist(Collection<BlockBean> list) {
        for (BlockBean blockBean : list) {
            System.out.println(blockBean.getTransactions().get(0).getAmount());
        }
        return "";
    }

    @RequestMapping(value = "/testMap", produces = "")
    public String testMap(Map<List<Transaction>, String> map) {
        return "";
    }

    @RequestMapping(value = "/testArray", produces = "")
    public String testMap(Map<List<Transaction>, String[]>[] map, List<String[]> list, int a, double b, float... fs) {
        return "";
    }

    @RequestMapping(value = "/mine", produces = "application/json")
    public Map<String, Object> mine() {
        String node_identifier = UUID.randomUUID().toString().replaceAll("-", "");
        BlockBean last_block = blockchain.last_block();
        int last_proof = last_block.getProof();
        int proof = blockchain.proof_of_work(last_proof);
        blockchain.new_transaction("0", node_identifier, new BigDecimal(1));
        String previous_hash = blockchain.hash(last_block);
        BlockBean blockBean = blockchain.new_block(previous_hash, proof);
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("message", "获得新的区块链");
        response.put("index", blockBean.getIndex());
        response.put("transactions", blockBean.getTransactions());
        response.put("proof", blockBean.getProof());
        response.put("previous_hash", blockBean.getPrevious_hash());
        return response;
    }
    @RequestMapping(value = "/transactions/new", produces = "application/json")
    public Map<String, String> new_transactions(Transaction transaction) {
        int index = blockchain.new_transaction(transaction.getSender(),transaction.getRecipient(),transaction.getAmount());
        Map<String, String> response = new HashMap<String, String>();
        response.put("message","新的交易信息被添加块"+index+"中");
        return response;
    }
    @RequestMapping(value = "/chain", produces = "application/json")
    public Map<String, Object> full_chain(){
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("chain", blockchain.getBlockBeans());
        response.put("length", blockchain.getBlockBeans().size());
        return response;
    }
    @RequestMapping(value = "/nodes/register", produces = "")
    public Map<String, Object> register_nodes(List<String> nodes){
        for (String node : nodes) {
            try {
                blockchain.register_node(node);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        Map<String, Object> response = new HashMap<String, Object>();
        response.put("message", "新的节点已经添加！");
        response.put("total_nodes", blockchain.getNodes());
        return response;
    }
    @RequestMapping(value = "/nodes/resolve", produces = "application/json")
    public Map<String, Object> consensus(){
        boolean replaced = blockchain.ressolve_conflicts();
        Map<String, Object> response = new HashMap<String, Object>();
        if(replaced) {
            response.put("message", "我们的区块链更新了！");
            response.put("new_chain", blockchain.getBlockBeans());
        }else{
            response.put("message", "我们的区块链无需更新！");
            response.put("new_chain", blockchain.getBlockBeans());
        }
        return response;
    }
}
