package com.webapp.userwebapp.controller;

import com.timgroup.statsd.StatsDClient;
import com.webapp.userwebapp.UserWebAppApplication;
import com.webapp.userwebapp.model.Product;
import com.webapp.userwebapp.model.User;
import com.webapp.userwebapp.repository.ProductRepository;
import com.webapp.userwebapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("/")
public class UserOps {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    ProductOps productOps;
    @Autowired
    private StatsDClient statsDClient;

    private static final Logger logger =  LoggerFactory.getLogger(UserWebAppApplication.class);

    @GetMapping("/healthz")
    public Object healthz()
    {

        //statsDClient.incrementCounter("healthz.service");
        return HttpStatus.OK;
    }
    @PostMapping("/v1/user")
    public Object createUser(@RequestBody User user)
    {
       try {
            //user.setUserId(user.getUserId());
            Object object;
            if (userRepository.existsByUsername(user.getUsername())) {
                object = HttpStatus.BAD_REQUEST;
                logger.error("Username already exists");
                return new ResponseEntity<>(object,HttpStatus.BAD_REQUEST);
            } else {
                //Base64.Encoder encoder = Base64.getEncoder();
                User usernew = new User();
                BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
                usernew.setFirstname(user.getFirstname());
                usernew.setLastname(user.getLastname());
                String pwd = passwordEncoder.encode(user.getPassword());
                //String password= encoder.encodeToString(user.getPassword().getBytes());

                usernew.setPassword(pwd);
                usernew.setUsername(user.getUsername());
                usernew.setAccount_created(LocalDateTime.now());
                usernew.setAccount_updated(LocalDateTime.now());
                userRepository.save(usernew);
                object = HttpStatus.CREATED;
                Map<String, String> userdetails = new HashMap<>();
                userdetails.put("id", String.valueOf(usernew.getUserId()));
                userdetails.put("username", usernew.getUsername());
                userdetails.put("firstname",usernew.getFirstname());
                userdetails.put("lastname",usernew.getLastname());
                userdetails.put("account_created",String.valueOf(usernew.getAccount_updated()));
                userdetails.put("account_updated",String.valueOf(usernew.getAccount_updated()));
                statsDClient.incrementCounter("create.user");
                Object object1 = userdetails;
                logger.info(usernew.getUsername() + "created successfully");
                return new ResponseEntity<>(object1,HttpStatus.CREATED);
            }
        }
        catch (Exception e)
        {
            Object object = HttpStatus.BAD_REQUEST;
            logger.error(e + "");
             return new ResponseEntity<>(object, HttpStatus.BAD_REQUEST);
        }
    }


    @GetMapping("/v1/user/{userId}")
    public Object getUserById(@PathVariable("userId") int userId) {
//       try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Optional<User> user = userRepository.findById(userId);
            String currentUsername = authentication.getName();
            Object object;
            if (user.get().getUsername().equals(currentUsername)) {

                Map<String, String> userdetails = new HashMap<>();


                //  if (user == null) {
                //    return HttpStatus.FORBIDDEN;
                //}
                // else if( ==  user.get().getUsername() )

                //else
                //{

                userdetails.put("id", String.valueOf(user.get().getUserId()));
                userdetails.put("firstname", user.get().getFirstname());
                userdetails.put("lastname", user.get().getLastname());
                userdetails.put("username", user.get().getUsername());
                userdetails.put("account_created", String.valueOf(user.get().getAccount_created()));
                userdetails.put("account_updated", String.valueOf(user.get().getAccount_updated()));
                object= userdetails;
                statsDClient.incrementCounter("getuserid.service");
                logger.info(userId + "details retrived successfully");
                return  new ResponseEntity<>(object,HttpStatus.OK);
            }
            else if (user.get().getUsername() != (currentUsername))
            {
                logger.error("Incorrect username");
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }
            else
            {
                logger.error("Incorrect username/password");
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }




        }
//        catch (NoSuchElementException e) {
//            return HttpStatus.FORBIDDEN;
//        }
        //Optional<User> userret = userRepository.findById(userId);

        // return userret;
//    }

    @PutMapping("/v1/user/{userId}")
    public ResponseEntity<Object> updateUser(@PathVariable int userId, @RequestBody User user) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = authentication.getName();
            Optional<User> user1 = userRepository.findById(userId);
            Object object;
            if (user == null) {
                object = HttpStatus.BAD_REQUEST;
                logger.error("Incorrect username");
                return new ResponseEntity<>(object,HttpStatus.BAD_REQUEST);
            } else if(user1.get().getUsername().equals(currentUsername)){
                BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
                String pwd = passwordEncoder.encode(user.getPassword());
                user1.map(userupdate -> {
                    userupdate.setFirstname(user.getFirstname());
                    userupdate.setLastname(user.getLastname());
                    userupdate.setPassword(pwd);
                    userupdate.setAccount_updated(LocalDateTime.now());
                    userRepository.save(userupdate);
                    statsDClient.incrementCounter("updateUser.service");
                    return null;
                });
                object = HttpStatus.NO_CONTENT;
                logger.info("User Details for userId" + userId + " updated.");
                return new ResponseEntity<>(object,HttpStatus.NO_CONTENT) ;

            }
            else if(user1.get().getUsername() != (currentUsername))
            {
                logger.error("Incorrect username");
                return new ResponseEntity<>(HttpStatus.FORBIDDEN) ;
            }
            else {
                logger.error("Invalid argument passed");
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }
        catch (Exception e) {
            Object object = HttpStatus.BAD_REQUEST;
            logger.error(e +"");
            return new ResponseEntity<>(object,HttpStatus.BAD_REQUEST);
        }
//        object =HttpStatus.UNAUTHORIZED;
//        return new ResponseEntity<>(object,HttpStatus.UNAUTHORIZED);
    }


    @GetMapping("/v1/product/{productId}")
    public Object getProductId(@PathVariable int productId)
    {
        try{
            return productOps.selectProduct(productId);

            } catch (Exception e) {

            return HttpStatus.BAD_REQUEST;
        }
    }

    @PostMapping("/v1/product")
    public Object addProduct(@RequestBody Product product)
    {

        return productOps.createProduct(product);
    }

    @PutMapping("/v1/product/{productId}")
    public Object PutProduct(@PathVariable int productId, @RequestBody Product product)
    {
        statsDClient.incrementCounter("putProduct.service");
        logger.info(product.getName() +" updated successfully");
        return productOps.PutProduct(productId,product);
    }

    @DeleteMapping("/v1/product/{productId}")
    public Object deleteProduct(@PathVariable int productId)
    {
        statsDClient.incrementCounter("deleteProduct.service");
        logger.info(productId +" deleted successfully");
        return productOps.DeleteProduct(productId);
    }
    @PatchMapping("/v1/product/{productId}")
    public Object PatchProduct(@PathVariable int productId, @RequestBody Product product)
    {
        statsDClient.incrementCounter("patchproduct.service");
        logger.info(product.getName() +" updated successfully");
        return productOps.updateProduct(productId,product);
    }


    public Integer getUserID (Integer id)
    {
        statsDClient.incrementCounter("getuser.service");
        Integer user = userRepository.getByUserId(id);
        return user;
    }

}
