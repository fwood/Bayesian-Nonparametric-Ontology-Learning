function embedding(Z,ind, words, current_embedding, fid)

m = (size(Z,1)+1);
left_node = Z(ind,1);
right_node = Z(ind,2);
this_embedding = [current_embedding '0, '];
if left_node <= m
    fprintf(fid,[words{left_node} ', ' this_embedding '\n']);
else
    embedding(Z,left_node-m,words,this_embedding,fid);
end

this_embedding = [current_embedding '1, '];
if right_node <= m
    fprintf(fid,[words{right_node} ', ' this_embedding '\n']);
else
    embedding(Z,right_node-m,words,this_embedding,fid);
end

    
%str
%pause(.01)

