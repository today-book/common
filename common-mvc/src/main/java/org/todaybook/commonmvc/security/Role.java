package org.todaybook.commonmvc.security;

public enum Role {
  USER,
  ADMIN;

  public static Role from(String value) {
    return Role.valueOf(value.toUpperCase());
  }
}
