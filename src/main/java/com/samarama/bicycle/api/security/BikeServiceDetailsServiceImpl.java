package com.samarama.bicycle.api.security;

import com.samarama.bicycle.api.model.BikeService;
import com.samarama.bicycle.api.repository.BikeServiceRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service("bikeServiceDetailsService")
public class BikeServiceDetailsServiceImpl implements UserDetailsService {
    private final BikeServiceRepository bikeServiceRepository;

    public BikeServiceDetailsServiceImpl(BikeServiceRepository bikeServiceRepository) {
        this.bikeServiceRepository = bikeServiceRepository;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        BikeService service = bikeServiceRepository.findByEmail(email)
                .orElseThrow(() -> new com.samarama.bicycle.api.exceptions.BikeServiceNotFoundException(email));

        return new org.springframework.security.core.userdetails.User(
                service.getEmail(),
                service.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_SERVICE"))
        );
    }
}