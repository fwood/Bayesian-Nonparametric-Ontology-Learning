textimportformat = '%s ';
returnexpression = '[words ';
datacompactionexpresison = '[';

for i = 1:600
    textimportformat = [textimportformat '%f '];
    returnexpression = [returnexpression 'v' num2str(i) ' '];
    datacompactionexpresison = [datacompactionexpresison 'v' num2str(i) ' '];
end
datacompactionexpresison = [datacompactionexpresison ']'];
returnexpression = [returnexpression ']'];

evalstr = [returnexpression '= textread(''../../data/chater_features.csv'',''' textimportformat ''',''delimiter'','','');'];
%evalstr = [returnexpression '= textread(''../../data/chater_features.csv'',''' textimportformat ''');'];

eval(evalstr);

evalstr2 = ['features = ' datacompactionexpresison ';'];

eval(evalstr2);

features = features./repmat(sum(features,2),1,600);

words = words(~isnan(sum(features,2)));
features = features(~isnan(sum(features,2)),:);

Sigma = cov(features);

[U,S,V] = svd(Sigma);

pca_features = features(1:5000,:)*(U(2,3,:)');
figure(2)
scatter(pca_features(:,1),pca_features(:,2))

D = pdist(features(1:10000,:));
Z = linkage(D,'average');
figure(1)
dendrogram(Z,0,'labels',words(1:10000),'orientation','left');

fid = fopen('../../data/chater_embeddings.csv','w');
embedding(Z,size(Z,1), words, '', fid);
fclose(fid);


