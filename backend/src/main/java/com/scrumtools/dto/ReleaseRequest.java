package com.scrumtools.dto;

import lombok.Data;

@Data
public class ReleaseRequest {
    private String name;
    private String description;
    /** Release Manager kullanıcı email'i; boş bırakılırsa oluşturan kişi atanır. */
    private String releaseManagerEmail;
    /** yyyy-MM-dd */
    private String freezeDate;
    /** yyyy-MM-dd */
    private String plannedReleaseDate;
}
