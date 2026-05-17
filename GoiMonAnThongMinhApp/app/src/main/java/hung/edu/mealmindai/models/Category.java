package hung.edu.mealmindai.models;

import com.google.firebase.Timestamp;

/**
 * Recipe category stored in the categories collection.
 */
public class Category {
    // Firestore document id or custom category id.
    private String categoryId;
    // Display information for grouping recipes.
    private String name;
    private String description;
    private String iconUrl;
    private String status;
    private String imageUrl;
    private Integer displayOrder;
    private Boolean active;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    /**
     * Required empty constructor for Firebase.
     */
    public Category() {
    }

    public Category(String categoryId, String name, String description, String imageUrl,
                    Integer displayOrder, Boolean active, Timestamp createdAt, Timestamp updatedAt) {
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIconUrl() {
        return iconUrl != null ? iconUrl : imageUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getStatus() {
        if (status != null) {
            return status;
        }
        return Boolean.FALSE.equals(active) ? "hidden" : "active";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}
