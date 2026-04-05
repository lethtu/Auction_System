package com.auction.server.model;

import jakarta.persistence.*;
@Entity
@DiscriminatorValue("admin")
public class Admin extends User {
}
