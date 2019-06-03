package com.latdev.netman;

import com.latdev.netman.utils.ParallelHTTP;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

class AppMain {

    public AppMain() {
        //
    }

    public void start() {
        ParallelHTTP req = new ParallelHTTP();
        Map<String,String> params = new HashMap<>();
        params.put("username", "GreathUser");
        params.put("password", "That is password");
        params.put("key[0]", "123");
        params.put("key[1]", "456");
        String result = req.post("http://localhost/work-time-flow/post.php?lap=magic", params);
        if (req.getLastError() != null) {
            Exception err = req.getLastError();
            err.getStackTrace();
            System.err.println(err.getMessage());
        }
        System.out.println(result);

        String result2 = req.get("http://localhost/work-time-flow/post.php?getcookie=yes");
        System.out.println(result2);
    }


    public static Integer noError(Callable<Integer> func) {
        try {
            return func.call();
        } catch (Exception err) {
            err.getStackTrace();
            System.err.println(err.getClass().getName() + ": " + err.getMessage());
        }
        return 0;
    }

}
