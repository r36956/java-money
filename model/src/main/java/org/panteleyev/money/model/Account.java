package org.panteleyev.money.model;

/*
 Copyright (c) Petr Panteleyev. All rights reserved.
 Licensed under the BSD license. See LICENSE file in the project root for full license information.
 */

import org.panteleyev.mysqlapi.annotations.Column;
import org.panteleyev.mysqlapi.annotations.ForeignKey;
import org.panteleyev.mysqlapi.annotations.PrimaryKey;
import org.panteleyev.mysqlapi.annotations.RecordBuilder;
import org.panteleyev.mysqlapi.annotations.ReferenceOption;
import org.panteleyev.mysqlapi.annotations.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

@Table("account")
public final class Account implements MoneyRecord, Named, Comparable<Account> {
    public final static Predicate<Account> FILTER_ALL = x -> true;
    public final static Predicate<Account> FILTER_ENABLED = Account::getEnabled;

    @PrimaryKey
    @Column("uuid")
    private final UUID guid;

    @Column("name")
    private final String name;

    @Column("comment")
    private final String comment;

    @Column("number")
    private final String accountNumber;

    @Column("opening")
    private final BigDecimal openingBalance;

    @Column("account_limit")
    private final BigDecimal accountLimit;

    @Column("rate")
    private final BigDecimal currencyRate;

    @Column("type_id")
    private final int typeId;

    @Column(value = "category_uuid")
    @ForeignKey(table = Category.class, column = "uuid", onDelete = ReferenceOption.RESTRICT)
    private final UUID categoryUuid;

    @Column("currency_uuid")
    @ForeignKey(table = Currency.class, column = "uuid", onDelete = ReferenceOption.RESTRICT)
    private final UUID currencyUuid;

    @Column("enabled")
    private final boolean enabled;

    @Column("interest")
    private final BigDecimal interest;

    @Column("closing_date")
    private final LocalDate closingDate;

    @Column("icon_uuid")
    @ForeignKey(table = Icon.class, column = "uuid", onDelete = ReferenceOption.SET_NULL)
    private final UUID iconUuid;

    @Column("card_type")
    private final CardType cardType;

    @Column("card_number")
    private final String cardNumber;

    @Column("created")
    private final long created;

    @Column("modified")
    private final long modified;

    private final CategoryType type;

    @RecordBuilder
    public Account(@Column("name") String name,
                   @Column("comment") String comment,
                   @Column("number") String accountNumber,
                   @Column("opening") BigDecimal openingBalance,
                   @Column("account_limit") BigDecimal accountLimit,
                   @Column("rate") BigDecimal currencyRate,
                   @Column("type_id") int typeId,
                   @Column("category_uuid") UUID categoryUuid,
                   @Column("currency_uuid") UUID currencyUuid,
                   @Column("enabled") boolean enabled,
                   @Column("interest") BigDecimal interest,
                   @Column("closing_date") LocalDate closingDate,
                   @Column("icon_uuid") UUID iconUuid,
                   @Column("card_type") CardType cardType,
                   @Column("card_number") String cardNumber,
                   @Column("uuid") UUID guid,
                   @Column("created") long created,
                   @Column("modified") long modified)
    {
        this.name = name;
        this.comment = comment;
        this.accountNumber = accountNumber;
        this.openingBalance = openingBalance;
        this.accountLimit = accountLimit;
        this.currencyRate = currencyRate;
        this.typeId = typeId;
        this.categoryUuid = categoryUuid;
        this.currencyUuid = currencyUuid;
        this.enabled = enabled;
        this.interest = interest;
        this.closingDate = closingDate;
        this.iconUuid = iconUuid;
        this.cardType = cardType;
        this.cardNumber = cardNumber;
        this.guid = guid;
        this.created = created;
        this.modified = modified;

        this.type = CategoryType.get(this.typeId);
    }

    public CategoryType getType() {
        return type;
    }

    @Override
    public int compareTo(Account other) {
        return name.compareToIgnoreCase(other.name);
    }

    public Account enable(boolean e) {
        return new Account.Builder(this)
            .enabled(e)
            .modified(System.currentTimeMillis())
            .build();
    }

    @Override
    public String getName() {
        return name;
    }

    public String getComment() {
        return comment;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }

    public BigDecimal getAccountLimit() {
        return accountLimit;
    }

    public BigDecimal getCurrencyRate() {
        return currencyRate;
    }

    public int getTypeId() {
        return typeId;
    }

    public UUID getCategoryUuid() {
        return categoryUuid;
    }

    public Optional<UUID> getCurrencyUuid() {
        return Optional.ofNullable(currencyUuid);
    }

    public boolean getEnabled() {
        return enabled;
    }

    public BigDecimal getInterest() {
        return interest;
    }

    public Optional<LocalDate> getClosingDate() {
        return Optional.ofNullable(closingDate);
    }

    public UUID getIconUuid() {
        return iconUuid;
    }

    public CardType getCardType() {
        return cardType;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    @Override
    public UUID getUuid() {
        return guid;
    }

    @Override
    public long getCreated() {
        return created;
    }

    @Override
    public long getModified() {
        return modified;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof Account)) {
            return false;
        }

        Account that = (Account) other;

        return Objects.equals(name, that.name)
            && Objects.equals(comment, that.comment)
            && Objects.equals(accountNumber, that.accountNumber)
            && openingBalance.compareTo(that.openingBalance) == 0
            && accountLimit.compareTo(that.accountLimit) == 0
            && currencyRate.compareTo(that.currencyRate) == 0
            && typeId == that.typeId
            && Objects.equals(categoryUuid, that.categoryUuid)
            && Objects.equals(currencyUuid, that.currencyUuid)
            && enabled == that.enabled
            && interest.compareTo(that.interest) == 0
            && Objects.equals(closingDate, that.closingDate)
            && Objects.equals(iconUuid, that.iconUuid)
            && cardType == that.cardType
            && Objects.equals(cardNumber, that.cardNumber)
            && Objects.equals(guid, that.guid)
            && created == that.created
            && modified == that.modified;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, comment, accountNumber, openingBalance.stripTrailingZeros(),
            accountLimit.stripTrailingZeros(), currencyRate.stripTrailingZeros(), typeId, categoryUuid, currencyUuid,
            enabled, interest, closingDate, iconUuid, cardType, cardNumber, guid, created, modified);
    }

    @Override
    public String toString() {
        return "Account ["
            + " uuid:" + guid
            + " name:" + name
            + " comment:" + comment
            + " accountNumber:" + accountNumber
            + " categoryUuid:" + categoryUuid
            + "]";
    }

    public String getAccountNumberNoSpaces() {
        return getAccountNumber().replaceAll(" ", "");
    }

    public String getCardNumberNoSpaces() {
        return getCardNumber().replaceAll(" ", "");
    }

    public static final class Builder {
        private String name = "";
        private String comment = "";
        private String accountNumber = "";
        private BigDecimal openingBalance = BigDecimal.ZERO;
        private BigDecimal accountLimit = BigDecimal.ZERO;
        private BigDecimal currencyRate = BigDecimal.ONE;
        private int typeId;
        private UUID categoryUuid;
        private UUID currencyUuid;
        private boolean enabled = true;
        private BigDecimal interest = BigDecimal.ZERO;
        private LocalDate closingDate = null;
        private UUID iconUuid = null;
        private CardType cardType = CardType.NONE;
        private String cardNumber = "";
        private UUID guid = null;
        private long created = 0;
        private long modified = 0;

        public Builder() {
        }

        public Builder(Account account) {
            if (account == null) {
                return;
            }

            name = account.getName();
            comment = account.getComment();
            accountNumber = account.getAccountNumber();
            openingBalance = account.getOpeningBalance();
            accountLimit = account.getAccountLimit();
            currencyRate = account.getCurrencyRate();
            typeId = account.getTypeId();
            categoryUuid = account.getCategoryUuid();
            currencyUuid = account.getCurrencyUuid().orElse(null);
            enabled = account.getEnabled();
            interest = account.getInterest();
            closingDate = account.getClosingDate().orElse(null);
            iconUuid = account.getIconUuid();
            cardType = account.getCardType();
            cardNumber = account.getCardNumber();
            guid = account.getUuid();
            created = account.getCreated();
            modified = account.getModified();
        }

        public UUID getUuid() {
            return guid;
        }

        public Account build() {
            if (guid == null) {
                guid = UUID.randomUUID();
            }

            long now = System.currentTimeMillis();
            if (created == 0) {
                created = now;
            }
            if (modified == 0) {
                modified = now;
            }

            return new Account(name, comment, accountNumber, openingBalance,
                accountLimit, currencyRate, typeId, categoryUuid,
                currencyUuid, enabled, interest, closingDate, iconUuid, cardType, cardNumber,
                guid, created, modified);
        }

        public Builder name(String name) {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Account name must not be empty");
            }
            this.name = name;
            return this;
        }

        public Builder comment(String comment) {
            this.comment = comment == null ? "" : comment;
            return this;
        }

        public Builder accountNumber(String accountNumber) {
            this.accountNumber = accountNumber == null ? "" : accountNumber;
            return this;
        }

        public Builder openingBalance(BigDecimal openingBalance) {
            this.openingBalance = openingBalance;
            return this;
        }

        public Builder accountLimit(BigDecimal accountLimit) {
            this.accountLimit = accountLimit;
            return this;
        }

        public Builder currencyRate(BigDecimal currencyRate) {
            this.currencyRate = currencyRate;
            return this;
        }

        public Builder typeId(int typeId) {
            this.typeId = typeId;
            return this;
        }

        public Builder categoryUuid(UUID categoryUuid) {
            this.categoryUuid = categoryUuid;
            return this;
        }

        public Builder currencyUuid(UUID currencyUuid) {
            this.currencyUuid = currencyUuid;
            return this;
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder interest(BigDecimal interest) {
            this.interest = interest;
            return this;
        }

        public Builder closingDate(LocalDate closingDate) {
            this.closingDate = closingDate;
            return this;
        }

        public Builder iconUuid(UUID iconUuid) {
            this.iconUuid = iconUuid;
            return this;
        }

        public Builder cardType(CardType cardType) {
            this.cardType = cardType;
            return this;
        }

        public Builder cardNumber(String cardNumber) {
            this.cardNumber = cardNumber == null ? "" : cardNumber;
            return this;
        }

        public Builder guid(UUID guid) {
            Objects.requireNonNull(guid);
            this.guid = guid;
            return this;
        }

        public Builder created(long created) {
            this.created = created;
            return this;
        }

        public Builder modified(long modified) {
            this.modified = modified;
            return this;
        }
    }
}
