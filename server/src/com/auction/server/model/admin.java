package com.auction.server.model;

import jakarta.persistence.*;
@DiscriminatorValue("admin")
public class admin extends user{
}
