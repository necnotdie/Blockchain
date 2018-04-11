import com.bc.block.BlockBean;
import com.bc.block.Transaction;
import com.bc.server.core.BlockThread;
import net.sf.ezmorph.bean.MorphDynaBean;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Test {
    public void get(List<String> a) {

    }

    public static void main(String[] args) {
        Transaction transaction = new Transaction();
        transaction.setAmount(new BigDecimal("5"));
        transaction.setRecipient("123");
        transaction.setSender("111");
        Transaction transaction1 = new Transaction();
        transaction.setAmount(new BigDecimal("5"));
        transaction.setRecipient("123");
        transaction.setSender("111");
        List<Transaction> transactions = new ArrayList<Transaction>();
        transactions.add(transaction);
        transactions.add(transaction1);
        BlockBean blockBean = new BlockBean();
        blockBean.setIndex(1);
        blockBean.setPrevious_hash("123");
        blockBean.setProof(123);
        blockBean.setTimestamp(123123);
        blockBean.setTransactions(transactions);
        List<BlockBean> blockBeans = new ArrayList<BlockBean>();
        blockBeans.add(blockBean);
        System.out.println(JSONArray.fromObject(transaction).toString());
        System.out.println(JSONArray.fromObject(blockBeans).toString());
        String result = JSONArray.fromObject(blockBeans).toString();
        List<BlockBean> beans = (List<BlockBean>) JSONArray.toCollection(JSONArray.fromObject(result), BlockBean.class);
//        for (BlockBean bean : beans){
//            System.out.println(bean.getProof());
//            System.out.println(bean.getTransactions().get(0).getAmount());
//        }
        System.out.println("123=".split("=").length);
        Object o = new String[]{"1", "2"};
        System.out.println(o.getClass().isArray());
        //        String result = JSONObject.fromObject(blockBean).toString();
//        BlockBean block = (BlockBean) JSONObject.toBean(JSONObject.fromObject(result),BlockBean.class);
//        System.out.println(block.getTimestamp());
        List<List<Transaction>> lists = new ArrayList<List<Transaction>>();
        List<Transaction>[] lists1 = new List[1];
        lists1[0] = transactions;
        lists.add(transactions);
        Map<String, List<List<Transaction>>> map = new HashMap<String, List<List<Transaction>>>();
        map.put("123", lists);
        Map<String, List<Transaction>[]> map2 = new HashMap<String, List<Transaction>[]>();
        map2.put("234", lists1);
        String result3 = JSONObject.fromObject(map2).toString();
        System.out.println(result3);
        Map<String,String> strmap = new HashMap<String, String>();
        strmap.put("123","123");
        String result4 = JSONObject.fromObject(strmap).toString();
        Map<String, Class> classMap = new HashMap<String, Class>();
        classMap.put("transactions", Transaction.class);
        MorphDynaBean morphDynaBean = (MorphDynaBean) JSONObject.toBean(JSONObject.fromObject(result4));
        try {
            Field field = MorphDynaBean.class.getDeclaredField("dynaValues");
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }
            System.out.println("class="+field.getClass());
            System.out.println("type="+field.getType().getGenericSuperclass());
            Map<String, Object> beanmap = (Map) field.get(morphDynaBean);
            System.out.println(((ParameterizedTypeImpl)beanmap.getClass().getGenericSuperclass()).getActualTypeArguments()[1]);
            org.objectweb.asm.Type asmType = org.objectweb.asm.Type.getType(beanmap.getClass());
            for (Map.Entry<String, Object> entry : beanmap.entrySet()) {
                System.out.println(entry.getKey());
                System.out.println(entry.getValue());
                System.out.println(entry.getValue().getClass());
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        System.out.println(map.get("123").get(0).get(0).getAmount());
        try {
            Method[] methods = Test.class.getDeclaredMethods();
            Method method = null;
            for (Method m : methods) {
                if ("get".equals(m.getName())) {
                    method = m;
                }
            }
            ParameterizedType parameterizedType = (ParameterizedType) method.getGenericParameterTypes()[0];
            Type type = parameterizedType.getActualTypeArguments()[0];

            System.out.println(parameterizedType.getClass());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
   /*public static void main(String args[]) throws NoSuchFieldException {
        Field field = BlockBean.class.getDeclaredField("previous_hash");
        if (!field.isAccessible()){
            field.setAccessible(true);
        }
       BlockThread.getFieldAsm(field);
    }*/
}
