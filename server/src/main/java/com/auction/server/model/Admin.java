package com.auction.server.model;

import jakarta.persistence.*;
@DiscriminatorValue("admin")
public class Admin extends User {
}
