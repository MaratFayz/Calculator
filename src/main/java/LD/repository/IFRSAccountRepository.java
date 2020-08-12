package LD.repository;

import LD.model.IFRSAccount.IFRSAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IFRSAccountRepository extends JpaRepository<IFRSAccount, Long>
{
}
