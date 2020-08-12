package LD.repository.CustomRepo;

/*public interface CustomEntryIFRSRepo extends SimpleJpaRepository<T, Integer>
{
	public <P> P calcAggregate(EntitySpecification<T> spec, SingularAttribute<?,P> column)
	{
		CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
		CriteriaQuery<P> query = criteriaBuilder.createQuery(column.getJavaType());

		if (spec != null)
		{
			Root<T> root = query.from(getDomainClass());
			query.where(spec.toPredicate(root, query, criteriaBuilder));
			query.select(criteriaBuilder.sum(root.get(column.getName())));
		}

		TypedQuery<P> typedQuery = em.createQuery(query);

		P result = typedQuery.getSingleResult();

		return result;
	}
}*/
